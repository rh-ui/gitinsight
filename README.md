<div align="center">
  <img src="logo.png" alt="GitInsight" width="120" />

  # GitInsight

  **Analyseur d'historique Git : mesurez la vélocité d'une équipe, repérez les fichiers à risque, révélez les zones détenues par une seule personne et les dépendances cachées entre fichiers — en CLI ou depuis un dashboard web.**

  Java 21 · Spring Boot 4 · JGit · Picocli · React 19 · TypeScript · Vite · Gradle

  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
  ![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk&logoColor=white)
  ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg?logo=springboot&logoColor=white)
  ![JGit](https://img.shields.io/badge/JGit-6.9-F05032.svg?logo=git&logoColor=white)
  ![React](https://img.shields.io/badge/React-19-61DAFB.svg?logo=react&logoColor=black)
  ![TypeScript](https://img.shields.io/badge/TypeScript-3178C6.svg?logo=typescript&logoColor=white)
  ![Vite](https://img.shields.io/badge/Vite-8-646CFF.svg?logo=vite&logoColor=white)
  ![Tailwind CSS](https://img.shields.io/badge/Tailwind-4-06B6D4.svg?logo=tailwindcss&logoColor=white)
  ![Gradle](https://img.shields.io/badge/Gradle-8.14-02303A.svg?logo=gradle&logoColor=white)

</div>

Un dépôt Git contient bien plus que du code : il contient **l'histoire de comment
l'équipe a construit ce code**. GitInsight lit cet historique et répond à des
questions qu'un `git log` ne montre pas :

- Quels fichiers concentrent le plus de risque de régression ?
- Que se passe-t-il si telle personne quitte l'équipe demain ?
- Quels fichiers changent *toujours* ensemble, alors que rien ne les relie dans le code ?

Deux interfaces, **un seul moteur d'analyse** : une CLI et un dashboard web.

---

## Sommaire

- [Aperçu](#aperçu)
- [Les 5 métriques](#les-5-métriques)
- [Architecture](#architecture)
- [Démarrage rapide](#démarrage-rapide)
- [La CLI](#la-cli)
- [L'API REST](#lapi-rest)
- [Le dashboard web](#le-dashboard-web)
- [Tests](#tests)
- [Choix techniques](#choix-techniques-notables)
- [Limites connues & suite](#limites-connues--suite)
- [Licence](#licence)

---

## Aperçu

<!-- TODO : remplacer par de vraies captures / un GIF de démo
![Dashboard — vue d'ensemble](docs/screenshots/overview.png)
![Graphe de couplage](docs/screenshots/coupling.png)
-->

```
╔════════════════════════════════════════╗
║          GitInsight — Rapport          ║
╚════════════════════════════════════════╝

  Dépôt    : D:\gitinsight
  Commits  : 24
  Période  : 2026-05-12 → 2026-07-24

── Fichiers à risque (hotspots) ─────────────────────

  Fichier                                    Commits   Auteurs     Risque
  ───────────────────────────────────────────────────────────────────────
  core/.../service/AnalysisService.java            9         2       18.0
  web/src/pages/CouplingPage.tsx                   6         1        6.0
```

---

## Les 5 métriques

| Métrique | Question à laquelle elle répond | Calcul |
|---|---|---|
| **Vélocité** | À quel rythme l'équipe livre-t-elle ? | Agrégation hebdomadaire : commits, lignes +/−, auteurs actifs |
| **Répartition par auteur** | Qui touche quoi, et combien ? | Commits, fichiers distincts touchés, lignes +/− par auteur |
| **Hotspots** | Où est la dette technique ? | `risque = nb de commits × nb d'auteurs distincts` |
| **Bus factor** | Quelles zones dépendent d'une seule personne ? | `git blame` au HEAD → part des lignes détenues par l'auteur dominant |
| **Couplage temporel** | Quels fichiers sont liés sans que le code le dise ? | Indice de Jaccard sur les co-modifications |

### Pourquoi ces formules

**Hotspot = fréquence × nombre d'auteurs.** Un fichier souvent modifié n'est pas
forcément un problème (un fichier de config change souvent). Un fichier modifié
souvent **par beaucoup de personnes différentes** l'est : personne n'en a la
maîtrise complète, et c'est là que les régressions apparaissent. Le score n'a pas
d'échelle absolue — il se lit relativement aux autres fichiers du même dépôt.

**Bus factor = ownership au HEAD.** On blâme les lignes réellement présentes
aujourd'hui, pas l'historique : un fichier écrit à 100 % par une personne partie
depuis est un risque *actuel*. Les blancs sont ignorés (`WS_IGNORE_ALL`) pour
qu'un reformatage ne fasse pas basculer la propriété d'un fichier entier.

**Couplage = Jaccard, pas comptage brut.**

```
score(A, B) = co-modifications(A, B) / (changements(A) + changements(B) − co-modifications(A, B))
```

Le comptage brut favoriserait mécaniquement les fichiers très actifs. Jaccard
normalise : deux fichiers modifiés 10 fois dont 9 ensemble sont bien plus couplés
que deux fichiers modifiés 500 fois dont 20 ensemble. Deux garde-fous :

- les paires vues **une seule fois** sont ignorées (bruit statistique) ;
- les commits touchant **plus de 50 fichiers** ne contribuent pas au pairage —
  merges et reformatages massifs créeraient un faux couplage généralisé, pour un
  coût en O(fichiers²).

---

## Architecture

```
gitinsight/
├── core/    ← moteur d'analyse — Java pur + JGit. Aucune dépendance à Spring.
├── api/     ← Spring Boot : expose le core en REST (sync + asynchrone).
├── cli/     ← Picocli : expose le core en ligne de commande.
└── web/     ← dashboard React / Vite / TypeScript (projet npm autonome).
```

**Le principe structurant : le `core` ne connaît ni Spring, ni la CLI, ni HTTP.**
C'est un module Java ordinaire qui prend un dépôt et rend un `RepositoryAnalysis`.
L'API et la CLI ne sont que des adaptateurs autour de lui. Conséquences concrètes :

- les calculs se testent sans lever de contexte Spring (les tests du core tournent
  en quelques centaines de ms sur des `CommitInfo` fabriqués à la main) ;
- ajouter une 3ᵉ interface ne demande aucune modification du moteur ;
- aucune logique métier ne peut fuir dans un contrôleur, il n'y a rien à y mettre.

### Le pipeline d'analyse

```
chemin d'un dépôt Git local
   └─> GitHistoryService ─── parcours de l'historique + diff par commit
          ├─> VelocityCalculator      (pur)
          ├─> AuthorStatsCalculator   (pur)
          ├─> HotspotCalculator       (pur)
          ├─> BlameService            (Git : blame borné au top 50)
          └─> CouplingCalculator      (pur)
                 └─> RepositoryAnalysis
```

Quatre des cinq calculateurs sont des fonctions pures sur la liste des commits :
un seul parcours du dépôt alimente toutes les métriques.

### Stack

| Couche | Technologie |
|---|---|
| Moteur | Java 21, JGit 6.9 |
| API | Spring Boot 4.1 (Web MVC + Validation) |
| CLI | Picocli 4.7, Jackson |
| Build back | Gradle 8.14 (Kotlin DSL), multi-module |
| Front | React 19, Vite 8, TypeScript, Tailwind 4 |
| Visualisation | Recharts, react-force-graph-2d |
| Tests | JUnit 5, AssertJ, MockMvc |

---

## Démarrage rapide

### Prérequis

- **JDK 21** (le toolchain Gradle le télécharge au besoin via le plugin foojay,
  mais Gradle 8.14 lui-même doit tourner sur un JDK ≤ 21 — pensez à pointer
  `JAVA_HOME` sur un JDK 21 si votre JDK par défaut est plus récent) ;
- **Node 20+** pour le dashboard ;
- **Git** installé.

### Tout lancer en local

```bash
./gradlew build
```

```bash
./gradlew :api:bootRun
```

L'API écoute sur `http://localhost:8080`. Dans un second terminal :

```bash
cd web && npm install && npm run dev
```

Le dashboard est servi sur `http://localhost:5173`. Créez `web/.env` :

```bash
echo "VITE_API_BASE_URL=http://localhost:8080/api" > web/.env
```

> Sous Windows, utilisez `gradlew.bat` à la place de `./gradlew`.

---

## La CLI

### Installation

```bash
./gradlew :cli:installDist
```

Le binaire est généré dans `cli/build/install/gitinsight/bin/`.

### Usage

```bash
gitinsight analyze [PATH] [--top N] [--json] [--ascii]
```

| Argument / option | Effet | Défaut |
|---|---|---|
| `PATH` | Chemin vers un dépôt Git local | `.` |
| `--top`, `-n` | Nombre de hotspots affichés | `10` |
| `--json` | Sortie JSON (même schéma que l'API) | rapport texte |
| `--ascii` | Rendu 100 % ASCII, pour les terminaux sans UTF-8 | Unicode |

```bash
gitinsight analyze . --top 20
```

```bash
gitinsight analyze ../junit5 --json > analyse.json
```

**Codes de sortie :** `0` succès · `1` erreur d'analyse (dépôt introuvable ou
vide) · `2` erreur d'usage.

---

## L'API REST

### `POST /api/analyze` — analyse synchrone

```json
{ "path": "/chemin/vers/repo", "topHotspots": 10, "topCoupling": 30 }
```

`path` est le chemin d'un dépôt Git local, lu par le serveur. `topCoupling` est
optionnel (défaut : 30). Réponse `200` :

```json
{
  "meta":      { "totalCommits": 842, "firstCommit": "…", "lastCommit": "…", "generatedAt": "…" },
  "velocity":  [ { "weekStart": "2026-07-20", "commits": 12, "linesAdded": 430, "linesDeleted": 118, "activeAuthors": 3 } ],
  "authors":   [ { "name": "…", "email": "…", "commits": 210, "filesTouched": 88, "linesAdded": 9120, "linesDeleted": 3400 } ],
  "hotspots":  [ { "path": "src/…/Service.java", "changeCount": 47, "distinctAuthors": 6, "riskScore": 282.0 } ],
  "busFactor": [ { "path": "src/…/Parser.java", "topAuthor": "…", "topAuthorLines": 512, "totalLines": 540, "ownership": 0.948 } ],
  "coupling":  [ { "fileA": "…", "fileB": "…", "coChanges": 18, "changesA": 24, "changesB": 21, "couplingScore": 0.66 } ]
}
```

### `POST /api/analyze/async` — analyse en tâche de fond

Même corps de requête. Répond `202` immédiatement :

```json
{ "jobId": "5f2c…" }
```

### `GET /api/analyze/status/{jobId}` — progression

```json
{ "status": "RUNNING", "step": "Bus factor", "current": 5, "total": 6, "analysis": null, "message": null }
```

`status` vaut `RUNNING`, `DONE` (le résultat complet est dans `analysis`) ou
`ERROR` (la raison est dans `message`).

**Pourquoi une version asynchrone ?** Sur un dépôt à l'historique fourni, le
blame et le couplage se comptent en dizaines de secondes — de quoi frôler le
timeout d'un navigateur ou d'un reverse-proxy. Le mode asynchrone rend la main
aussitôt et permet au dashboard d'afficher une progression réelle (étape par
étape) plutôt qu'un sablier. La version synchrone reste disponible pour les
scripts.

### Erreurs

Réponse d'erreur uniforme, produite par un `@RestControllerAdvice` :

```json
{ "status": 400, "error": "Bad Request", "message": "le champ 'path' est obligatoire" }
```

### CORS

Origines autorisées configurables sans recompiler :

```bash
./gradlew :api:bootRun --args="--gitinsight.cors.allowed-origins=https://mon-front.example"
```

Défaut : `http://localhost:5173`.

---

## Le dashboard web

Cinq vues, une par métrique, alimentées par une seule analyse mise en contexte
React (`AnalysisProvider`) :

| Route | Contenu |
|---|---|
| `/overview` | Cartes de synthèse + courbe de vélocité (Recharts) |
| `/authors` | Contribution par auteur |
| `/hotspots` | Table triable et paginée des fichiers à risque |
| `/bus-factor` | Fichiers classés par concentration de propriété |
| `/coupling` | Graphe de réseau interactif + top couplages, filtrable par seuil |

Types TypeScript stricts, miroir des `record` Java du module `core`
([`web/src/types/analysis.ts`](web/src/types/analysis.ts)) : un changement de
schéma côté back casse la compilation du front plutôt que l'affichage à l'exécution.
Thème clair / sombre inclus.

---

## Tests

```bash
./gradlew test
```

- **`core`** — chaque calculateur a ses tests unitaires, écrits **avant** son
  exposition par l'API. Un utilitaire `GitTestRepo` construit à la volée de vrais
  petits dépôts Git en dossier temporaire : les tests d'historique et de blame
  s'exécutent contre du vrai Git, pas contre des mocks, et restent hermétiques.
- **`api`** — tests de contrôleur MockMvc : contrat HTTP, validation, mapping
  d'erreurs.
- **`cli`** — parsing des options, codes de sortie, rendu texte et JSON.

---

## Choix techniques notables

**Le blame est borné, volontairement.** `git blame` coûte O(historique du fichier).
Blâmer un arbre entier sur un gros dépôt est hors de question. On ne blâme donc que
les **50 fichiers les plus modifiés** — statistiquement ceux où la concentration de
connaissance importe. Les fichiers disparus du HEAD sont ignorés silencieusement
plutôt que de faire échouer l'analyse.

**L'analyse porte sur des dépôts déjà présents sur le disque.** Le moteur ne
clone rien : il ouvre un dossier local. C'est ce qui garde le `core` sans I/O
réseau, donc testable hors-ligne et sans timeout à gérer.

**Le rendu CLI a un mode ASCII.** Les consoles Windows en page de code héritée
massacrent les caractères Unicode ; `--ascii` replie accents et barres de
progression sur des octets ≤ 0x7F, lisibles partout.

---

## Limites connues & suite

- Les jobs d'analyse sont stockés **en mémoire, sans éviction TTL** : suffisant
  pour un outil mono-instance, à revoir avant une mise en production multi-instance.
- Les `git blame` sont **séquentiels** — les paralléliser est le gain de perf le
  plus évident sur gros dépôt.
- Pas de **déduplication d'identités** : un même contributeur avec deux adresses
  e-mail compte pour deux auteurs (un `.mailmap` serait la piste).
- Pas de filtre par branche, période ou sous-dossier : l'analyse porte sur tout
  l'historique atteignable depuis HEAD.

**Reste à faire (phase 6) :** pipeline CI GitHub Actions, déploiement en ligne du
back et du front, captures et GIF de démo.

---

## Licence

Distribué sous licence **MIT** — voir [LICENSE](LICENSE).
