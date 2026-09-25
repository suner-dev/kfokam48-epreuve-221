# SOUMISSION — Épreuve finale fullstack KFOKAM48

> Fichier à téléverser sur la plateforme. Les deux hash ci-dessous ont été relevés **après** le
> dernier push, comme l'impose le sujet : la correction porte exactement sur les commits
> déclarés, tout push ultérieur est ignoré.

## Candidat

| Champ | Valeur |
|---|---|
| Nom et prénom(s) | **Ngansop Sumo Rainer** |
| Matricule | **221** |
| Centre | **Yaoundé** |
| Compte GitHub | **suner-dev** |

## Projet

| Champ | Valeur |
|---|---|
| URL du dépôt public | `https://github.com/suner-dev/kfokam48-epreuve-221` |
| Branche | `main` |
| Hash du dernier commit (40 caractères) | `f25f4b5bcfbd0a137d2f3ad025c522d01a08cfb5` |
| Dernier commit | `[JALON] v1.0` |

## Épreuve Git (étape 5)

**Annulée.** Aucun second dépôt n'a été créé : `kfokam48-gitlab-221` n'existe pas et il n'y a
donc pas de hash à déclarer. L'étape 5 ne compte pas dans le barème.

## Technique

| Champ | Valeur |
|---|---|
| Frontend | **Angular 22**, justifié dans le `README` : son socle (DI, `HttpClient`, RxJS) impose une couche de service dédiée qui centralise les appels API, ce qui est la contrainte F3 |
| Backend | Spring Boot 3.5.3, Java 17, Maven, wrapper `mvnw` commité |
| Base de données | **PostgreSQL 16** à l'exécution et au développement local, **H2** pour tous les tests automatisés — aucun test ne dépend d'une base locale |
| Migrations | Flyway `V1` à `V3`, versionnées et commitées, `ddl-auto=validate` |
| Commandes de démarrage | `docker compose up -d --build` puis `cd frontend && npm ci && npm start` — **2 commandes**, avec données de démonstration chargées au démarrage |

## Ce que j'ai livré

- **Le besoin complet en deux versions** : ouverture de session avec code expirant, marquage de
  présence par code, dépôt du lien d'exercice, relecture par un pair, tableau de synthèse par
  promotion — plus les écrans Angular formateur, étudiant et relecteur, la fin et la clôture de
  session, la présence manuelle du formateur, l'anonymat du relecteur, la correction de note et le
  remplacement de lien, l'anti-devinette et le diagramme D4 (bonus).
- **Ce qui marche et qui est prouvé** : 30 tests unitaires et 34 tests d'intégration, tous verts
  sur `./backend/mvnw clean verify`, plus un relevé des 17 appels manuels du contrat rejoué sur
  l'application Docker et consigné dans `docs/APPELS_MANUELS.md`.
- **Ce que j'ai laissé de côté, et pourquoi** : **le changement de besoin de l'enveloppe** — deux
  pairs par exercice avec moyenne des deux notes et mention provisoire. L'analyse, la migration, le
  contrat et le backend sont écrits et prouvés par 8 tests sur la branche
  `feature/70-deux-relecteurs`, mais la base de test H2 ne sait pas supprimer l'index unique d'une
  contrainte, ce qui rend la migration invérifiable. **En production, un seul relecteur par
  exercice reste donc en vigueur** et la règle Q6 n'est pas annulée. Je préfère un périmètre
  réduit et annoncé à une promesse non tenue.
- **Également hors périmètre, assumé** : la coquille applicative « verre » (issues #58 à #61), dont
  le sacrifice est écrit en section 3 du cahier des charges — le rendu visuel n'est pas noté.
- **Limite structurelle assumée** : l'anti-devinette s'applique par `etudiantId` et non par
  appareil, faute d'authentification (Q1). Documenté en H4.

## Les trois jalons, dans l'ordre

| Jalon | Ordre dans l'historique | Présent |
|---|---|---|
| `[JALON] depart` | 1 | ✅ |
| `[JALON] analyse` | 5 | ✅ — avant tout commit de code |
| `[JALON] v0.1` | 40 | ✅ |
| `[JALON] v1.0` | 90 | ✅ |

## Checklist avant téléversement

- [x] Dépôt public, ouvert et testé en navigation privée
- [x] Hash de 40 caractères existant sur GitHub et relu **après** le dernier push
- [x] `git status` propre, tout est poussé
- [x] `README` testé depuis un dossier vide, sans `.env` préalable
- [x] `JOURNAL.md` et `CAHIER_DES_CHARGES.md` présents dans `docs/`
- [x] Les trois jalons poussés et dans le bon ordre
- [x] Aucun `target/`, `node_modules/` ou `dist/` suivi ; aucun secret dans l'historique

## Déclaration

Je déclare que ce dépôt est mon travail, que les commandes et les résultats rapportés dans ce
fichier et dans `docs/APPELS_MANUELS.md` ont été réellement exécutés, et que les limites
annoncées ci-dessus le sont volontairement et non par omission.

**Signature :** Ngansop Sumo Rainer · 221 · Yaoundé
**Date :** 25/09/2026
