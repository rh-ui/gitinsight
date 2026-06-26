# Phase 2 — Métriques + API (mode tuteur : tu codes, je guide)

## Contexte

La Phase 1 est terminée et solidifiée : `core` lit l'historique d'un dépôt Git
(`GitHistoryService.getHistory(path, limit)`) et le projette en `CommitInfo`,
avec des tests hermétiques. La Phase 2 (cf. `PROJECT.md` §3 et §5) transforme ces
données brutes en **insights** et les expose via une **API REST**.

Objectif de ce pas : implémenter dans `core` les 3 premières métriques —
**Vélocité**, **Répartition par auteur**, **Hotspots** — chacune testée
unitairement, puis monter l'API Spring Boot `POST /api/analyze` qui renvoie un
JSON structuré. On démarre par la Vélocité (ordre PROJECT.md).

**Mode de travail :** je t'explique le design et le *pourquoi* (matière à
entretien) à chaque étape, **tu écris le code**, je relis avant de passer à la
suite. On avance étape par étape, pas tout d'un coup.

---

## Principes d'architecture (à respecter sur toute la Phase 2)

1. **Calculateurs purs et sans Git.** Chaque métrique est une fonction
   `compute(List<CommitInfo>) -> Résultat`. Elle ne connaît ni JGit ni Spring →
   tests ultra-rapides avec des commits fabriqués à la main, sans dépôt.
   C'est le cœur du principe « moteur testable » de PROJECT.md.
2. **Une seule lecture d'historique.** Un `AnalysisService` lit les commits
   **une fois** et alimente les 3 calculateurs (évite 3 parcours du repo —
   c'est le point perf du §6).
3. **Pas de logique dans le contrôleur.** Le contrôleur Spring appelle
   `AnalysisService` et rien d'autre.
4. **Modéliser richement une fois.** On enrichit `CommitInfo` pour porter les
   stats de lignes, au lieu de re-differ plus tard.

---

## Étape 0 — Plomberie (prérequis de la Vélocité)

La Vélocité a besoin de **lignes ajoutées/supprimées** (que `changedFiles`
n'a pas) et de **tout l'historique** (pas seulement N commits).

**0a. Nouveau record `FileChange`** (`core/.../model/FileChange.java`)
```
record FileChange(String path, ChangeType type, int linesAdded, int linesDeleted)
```
(`type` = enum simple ADD/MODIFY/DELETE/RENAME, mappé depuis `DiffEntry.ChangeType`.)

**0b. Enrichir `CommitInfo`** : `List<String> changedFiles` → `List<FileChange> changes`.
- Modifier `GitHistoryService.changedFiles(...)` (`GitHistoryService.java:82`) pour
  calculer les lignes via `diffFormatter.toFileHeader(entry).toEditList()`, en
  sommant par `Edit` : ajoutées `+= e.getEndB()-e.getBeginB()`, supprimées
  `+= e.getEndA()-e.getBeginA()`. `pathOf(entry)` (`:108`) reste la source du chemin.
- Adapter les tests existants `GitHistoryServiceTest` (les `containsExactly("...")`
  deviennent des assertions sur `FileChange`).

**0c. Lecture de l'historique complet** : ajouter `getHistory(Path repoPath)`
(sans `limit`) qui ne pose pas de `setMaxCount`. Les métriques le consommeront.
(Streaming = optimisation ultérieure ; une `List` complète suffit pour démarrer.)

---

## Étape 1 — Vélocité (core + tests)

**Modèle :** `record WeeklyVelocity(LocalDate weekStart, int commits, int linesAdded,
int linesDeleted, int activeAuthors)` ; la métrique renvoie une `List<WeeklyVelocity>`
triée par semaine.

**Calculateur :** `VelocityCalculator.compute(List<CommitInfo>) : List<WeeklyVelocity>`
- Bucket par **semaine ISO** : `commit.date()` (Instant) → `LocalDate` en UTC →
  lundi de la semaine (`with(DayOfWeek.MONDAY)` via `WeekFields.ISO`).
- Par bucket : nb commits, somme lignes ajoutées/supprimées (sur tous les
  `FileChange`), nb d'auteurs distincts (`authorEmail`).

**Tests purs** (`VelocityCalculatorTest`, sans Git) : fabriquer des `CommitInfo`
sur 2–3 semaines, vérifier le regroupement, les sommes de lignes, le compte
d'auteurs actifs, et le tri chronologique. Cas limites : semaine sans commit,
commit sur la frontière dimanche/lundi.

---

## Étape 2 — Répartition par auteur (core + tests)

**Modèle :** `record AuthorStats(String name, String email, int commits,
int filesTouched, int linesAdded, int linesDeleted)`.

**Calculateur :** `AuthorStatsCalculator.compute(List<CommitInfo>) : List<AuthorStats>`
- Grouper par `authorEmail` ; `filesTouched` = nb de chemins distincts touchés
  par l'auteur ; sommer les lignes. Trier par commits décroissant.

**Tests purs :** plusieurs auteurs, vérifier comptes, fichiers distincts, lignes.

---

## Étape 3 — Hotspots (core + tests)

**Modèle :** `record Hotspot(String path, int changeCount, int distinctAuthors,
double riskScore)` avec `riskScore = changeCount * distinctAuthors`.

**Calculateur :** `HotspotCalculator.compute(List<CommitInfo>, int topN) : List<Hotspot>`
- Par chemin : compter les commits qui le touchent + ensemble des `authorEmail`
  distincts. Trier par `riskScore` décroissant, garder `topN`.

**Tests purs :** fichier modifié par 1 vs plusieurs auteurs → vérifier le score
et l'ordre du top-N.

---

## Étape 4 — Orchestration (core + test d'intégration)

**Modèle agrégat :** `record RepositoryAnalysis(AnalysisMeta meta,
List<WeeklyVelocity> velocity, List<AuthorStats> authors, List<Hotspot> hotspots)`
+ `record AnalysisMeta(int totalCommits, Instant firstCommit, Instant lastCommit,
Instant generatedAt)`.

**Service :** `AnalysisService.analyze(Path repo, int topHotspots) : RepositoryAnalysis`
- Lit l'historique **une fois** (Étape 0c), appelle les 3 calculateurs, assemble.

**Test d'intégration :** `@TempDir` + repo fabriqué (réutiliser les helpers de
`GitHistoryServiceTest`), vérifier que l'analyse complète est cohérente.

---

## Étape 5 — API Spring Boot

**Build (`api/build.gradle.kts`)** : ajouter le plugin Spring Boot
(`org.springframework.boot` + `io.spring.dependency-management`) et
`spring-boot-starter-web`. Garder `implementation(project(":core"))`.
Le plugin Spring Boot se déclare dans le bloc `plugins {}` du module `api`
(version, p.ex. 3.3.x).

**Code (`api/.../`)** :
- `GitInsightApplication` (`@SpringBootApplication`, `main`).
- `AnalyzeController` : `POST /api/analyze`, corps `{"path": "...", "topHotspots": 10}`,
  appelle `AnalysisService.analyze(...)`, renvoie `RepositoryAnalysis` (Jackson
  sérialise les records ; `Instant` géré par jackson-jsr310 fourni par le starter).
- Gestion d'erreur : chemin inexistant / pas un dépôt Git → **400** avec message
  clair (`@ExceptionHandler` ou `ResponseStatusException`), au lieu d'une 500.
- **Note sécurité à documenter** : accepter un chemin local arbitraire est
  sensible (lecture de dépôts hors périmètre). Pour l'instant chemin local +
  validation ; le clonage d'URL distante (clone en dossier temp) est un *plus*
  pour plus tard.

**Test :** `@WebMvcTest(AnalyzeController.class)` + `MockMvc`, `AnalysisService`
mocké → vérifier le JSON et le 400 sur chemin invalide.

---

## Fichiers concernés

**Créés (core) :** `model/FileChange.java`, `model/WeeklyVelocity.java`,
`model/AuthorStats.java`, `model/Hotspot.java`, `model/RepositoryAnalysis.java`,
`model/AnalysisMeta.java`, `metric/VelocityCalculator.java`,
`metric/AuthorStatsCalculator.java`, `metric/HotspotCalculator.java`,
`service/AnalysisService.java` (+ leurs tests).

**Modifiés (core) :** `model/CommitInfo.java`, `service/GitHistoryService.java`,
`service/GitHistoryServiceTest.java`.

**Créés/modifiés (api) :** `build.gradle.kts`, `GitInsightApplication.java`,
`web/AnalyzeController.java` (+ test).

---

## Vérification (fin de Phase 2)

1. `export JAVA_HOME="/c/Program Files/Java/jdk-21"` puis
   `./gradlew test` → tous les tests core + api au vert.
2. `./gradlew :api:bootRun`, puis dans un autre terminal :
   `curl -X POST localhost:8080/api/analyze -H "Content-Type: application/json"
   -d '{"path":"D:/GitInsight","topHotspots":10}'` → JSON avec velocity / authors
   / hotspots cohérents sur le repo GitInsight lui-même.
3. Vérifier un cas d'erreur : chemin bidon → **400** avec message lisible.
4. Commit propre en Conventional Commits (p.ex. `feat(core): métriques vélocité,
   auteurs, hotspots` puis `feat(api): endpoint POST /api/analyze`).

---

## Déroulé interactif

Comme tu codes toi-même, on avancera **étape par étape** : pour chaque étape je
te donne le design détaillé + les cas de test à écrire, tu implémentes, je relis,
on commite, on enchaîne. On commence par l'**Étape 0a** (record `FileChange`)
dès que tu valides ce plan.