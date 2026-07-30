# GitInsight — Analyseur de dépôts Git

> Document de référence du projet. À garder à la racine du repo.
> Claude Code doit le lire au début de **chaque** session de travail.

---

## 1. Vision du projet

**GitInsight** est un outil qui analyse l'historique d'un dépôt Git et en extrait
des insights d'ingénierie : points chauds de dette technique, concentration de
connaissance (bus factor), couplage entre fichiers, vélocité d'équipe.

Le produit a **deux interfaces** sur un **moteur d'analyse commun** :
- une **CLI** qui produit un rapport dans le terminal ou un export JSON ;
- un **dashboard web** (React) qui visualise les métriques.

Objectif personnel : projet de portfolio démontrant back-end, front-end, infra,
et une vraie logique métier. Doit être déployé, testé et documenté.

---

## 2. Stack technique

| Couche            | Technologie                              |
|-------------------|------------------------------------------|
| Moteur + API      | Java 21, Spring Boot 3.x                 |
| Parsing Git       | JGit (`org.eclipse.jgit`)                |
| CLI               | Picocli                                  |
| Build             | Gradle (Kotlin DSL)                      |
| Front             | React 18 + Vite + TypeScript             |
| Graphiques        | Recharts                                 |
| Tests back        | JUnit 5 + AssertJ                        |
| CI                | GitHub Actions                           |

**Principe d'architecture clé :** le moteur d'analyse est un module Java pur,
**sans aucune dépendance à Spring ni à la CLI**. La CLI et l'API ne sont que des
adaptateurs autour de ce moteur. Ça garde le cœur testable et réutilisable.

```
gitinsight/
├── core/        ← moteur d'analyse (Java pur, JGit). Le cerveau.
├── api/         ← Spring Boot, expose le core en REST.
├── cli/         ← Picocli, expose le core en ligne de commande.
└── web/         ← front React/Vite.
```

---

## 3. Les métriques (le cœur de la valeur)

À implémenter dans cet ordre de priorité :

1. **Vélocité** — commits/semaine, lignes ajoutées/supprimées dans le temps,
   nombre de contributeurs actifs par période.
2. **Répartition par auteur** — qui touche quels fichiers/dossiers, surface de
   contribution par personne.
3. **Fichiers à risque (hotspots)** — croisement entre fréquence de changement
   (nb de commits touchant le fichier) et nombre d'auteurs distincts. Un fichier
   très modifié par beaucoup de monde est un point chaud de dette.
4. **Bus factor** — pour chaque fichier/dossier, quel % des lignes vient d'un
   seul auteur. Révèle les zones fragiles.
5. **Couplage temporel** — quels fichiers changent souvent ensemble dans les
   mêmes commits. Révèle des dépendances cachées.

Les métriques 4 et 5 sont les plus différenciantes : peu de projets les font.

---

## 4. Conventions de code

- **Commits** : format Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`,
  `refactor:`, `chore:`).
- **Branches** : une branche par phase (`phase-1-core`, `phase-2-metrics`, …).
- **Tests d'abord pour le moteur** : toute métrique a ses tests unitaires AVANT
  d'être exposée par l'API. On ne fait pas confiance à un calcul non testé.
- **Pas de logique métier dans les contrôleurs** : les contrôleurs Spring
  appellent le `core` et rien d'autre.
- **TypeScript strict** côté front, types explicites pour les réponses API.

---

## 5. Planning — 6 phases

> Une phase ≈ une semaine, mais avance à ton rythme. Chaque phase se termine par
> un livrable démontrable et un commit propre.

### Phase 1 — Le moteur de base
**But :** prouver qu'on sait lire un dépôt Git.
- Mettre en place le multi-module Gradle (`core`, `api`, `cli`, `web`).
- Intégrer JGit dans `core`.
- Écrire un service qui ouvre un dépôt local et parcourt tout l'historique :
  pour chaque commit → hash, auteur, date, message, fichiers modifiés.
- Sortie attendue : un test/un main qui liste les N derniers commits d'un repo.
- **Livrable :** `core` extrait correctement l'historique d'un vrai repo.

### Phase 2 — Les métriques + l'API
**But :** transformer les données brutes en insights et les exposer.
- Implémenter Vélocité, Répartition par auteur, Hotspots dans `core`.
- Tests unitaires sur chaque calcul (avec un petit repo de test fixe).
- Monter l'API Spring Boot : `POST /api/analyze` reçoit un chemin de repo,
  renvoie un JSON structuré avec toutes les métriques.
- **Livrable :** appel REST → JSON d'analyse complet.

### Phase 3 — La CLI
**But :** première interface utilisable.
- Commande Picocli `gitinsight analyze <path>` avec options `--json`, `--top N`.
- Rapport lisible dans le terminal (tableaux des hotspots, vélocité).
- **Livrable :** binaire CLI qui produit un rapport sur n'importe quel repo local.

### Phase 4 — Dashboard, partie 1
**But :** rendre les insights visuels.
- Front React/Vite + client API typé.
- Vue d'ensemble du repo, graphe de vélocité (Recharts), tableau triable des
  fichiers à risque.
- **Livrable :** dashboard fonctionnel sur les 3 premières métriques.

### Phase 5 — Dashboard, partie 2 + métriques avancées
**But :** la partie qui impressionne.
- Implémenter Bus factor et Couplage temporel dans `core` (+ tests).
- Les exposer dans l'API et les visualiser (matrice ou graphe de couplage).
- Soigner le design — un dashboard moche tue un bon projet.
- **Livrable :** les 5 métriques visibles et bien présentées.

### Phase 6 — Finition et déploiement
**But :** le passage de « projet perso » à « projet de portfolio ».
- README excellent : problème, solution, captures, GIF de démo, choix techniques.
- Pipeline CI GitHub Actions (build + tests sur chaque push).
- Déploiement réel (back + front accessibles en ligne).
- Article court sur une difficulté technique résolue (perf sur gros repo, ou
  l'algo de couplage temporel).
- **Livrable :** projet déployé, testé, documenté, partageable.

---

## 6. Défis techniques à anticiper (sujets d'entretien en or)

- **Performance sur gros repos** : parcourir des dizaines de milliers de commits
  demande du streaming, du cache, peut-être de la parallélisation. Tester tôt sur
  un gros repo open source (le repo de Spring, de React…).
- **Algorithme de couplage temporel** : comment compter efficacement les paires
  de fichiers co-modifiés sans exploser en mémoire.
- **Calcul du bus factor** : nécessite un `git blame` par fichier — coûteux,
  à optimiser.

---

## 7. Règles de collaboration avec Claude Code

1. **Une phase à la fois.** Ne pas demander tout le projet d'un coup. Terminer,
   tester et committer une phase avant la suivante.
2. **Toujours tester le moteur** avant de passer à l'interface.
3. **Demander à Claude d'expliquer ses choix d'architecture** quand ils ne sont
   pas évidents — c'est ce que tu réutiliseras en entretien.
4. **Committer souvent**, en Conventional Commits.
5. Relire ce document au début de chaque session pour rester aligné.

---

## 8. Définition de « terminé » (pour chaque phase)

- [ ] Le code compile et tourne.
- [ ] Les tests passent (et il y en a pour toute logique métier).
- [ ] Le livrable de la phase est démontrable.
- [ ] Commit propre en Conventional Commits.
- [ ] (Phases avec front) ça ne ressemble pas à un template par défaut.
