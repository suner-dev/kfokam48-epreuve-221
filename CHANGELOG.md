# Journal des versions — KFOKAM48

> Ce fichier est **cohérent avec l'historique Git** : chaque version ci-dessous correspond à des
> commits réels, vérifiés par `git log`. Aucune entrée n'y est annoncée sans commits derrière.
> Auteur : Ngansop Sumo Rainer · 221.

## Jalons

| Jalon | Commit | Contenu |
|---|---|---|
| `[JALON] depart` | test de canal | Vérification que le dépôt distant accepte un push |
| `[JALON] analyse` | étape 1 | Cahier des charges, 4 diagrammes, backlog, contrat figé — **avant** tout code |
| `[JALON] v0.1` | étape 2 | Backend Spring Boot, frontend Angular, 13 tickets Must livrés par branche et PR |
| `[JALON] v1.0` | étape 4 | Documentation, backlog trié, tests et appels manuels vérifiés |

## Étape 1 — Analyse (spécification seule)

- Cahier des charges en 10 sections imposées : **16 exigences fonctionnelles**, **19 règles de
  gestion**, 1 contradiction tranchée (Q10 contre Q15) et 10 zones d'ombre.
- Contradiction **Q10 vs Q15** tranchée en faveur de Q10 : une règle détaillée et bornée dans le
  temps l'emporte sur une intention générale. Justification écrite en section 7.
- Trou principal comblé : **la clôture de session**, supposée partout (Q10, Q11, Q12) et définie
  nulle part. Hypothèse H1 écrite, avec son endpoint et sa règle RG15.
- Diagrammes D1, D2, D3 et **D4 (bonus)** en Mermaid versionné.
- `api/contrat.yaml` complété et figé : les 5 opérations imposées sont préservées mot pour mot,
  12 opérations libres ajoutées sous `/api`.
- `.gitignore` Java et JS posé **avant** le premier commit de code.

## Étape 2 — Première version

### Backend

| Version | Changement |
|---|---|
| v0.1 | Socle Spring Boot : POM, wrapper `mvnw` commité, structure des packages, PostgreSQL à l'exécution et H2 pour les tests, profils séparés, migrations Flyway, gestion centralisée des erreurs `{code,message}`, DTO systématiques |
| v0.1 | EF1 — ouverture d'une session avec code expirant à +15 min |
| v0.1 | EF2 — marquage de présence par code, expiration, fin, clôture, doublon |
| v0.1 | EF3 — dépôt d'un lien, unicité session/étudiant, affectation unique |
| v0.1 | EF4 — affectation automatique d'un pair parmi les présents, jamais l'auteur |
| v0.1 | EF5 — rendu d'une note entière 0–20 et d'un commentaire, anti auto-relecture |
| v0.1 | EF6 — tableau par promotion, moyenne calculée par l'API |
| v0.1 | EF8 — présence manuelle du formateur, distincte par sa `source` |
| v0.1 | EF9 / EF10 — remplacement du lien et correction de la note avant clôture (arbitrage Q10, Q13) |
| v0.1 | EF12 — anti-devinette : blocage 2 min après 5 saisies invalides |
| v0.1 | EF13 — sélection d'un étudiant dans une liste, sans mot de passe |
| v0.1 | EF14 — anonymat de l'identité du relecteur, vérifié sur le JSON |
| v0.1 | EF15 — fin de session puis clôture, deux événements distincts |
| v0.1 | EF16 — liste des relectures à faire et démarrage d'une relecture |
| v0.1 | D4 — statuts d'exercice visibles au formateur (bonus) |
| v0.1 | ENF5 — jeu de démonstration réaliste : 60 étudiants, 2 sessions, 4 exercices |

### Frontend

| Version | Changement |
|---|---|
| v0.1 | Angular 22 en TypeScript strict, trois écrans, routage et couche `services/` dédiée |
| v0.1 | Écran formateur : ouverture, sessions, fin et clôture, présence manuelle, tableau, détails |
| v0.1 | Écran étudiant : choix d'identité, présence, dépôt du lien |
| v0.1 | Écran relecteur : relectures à faire, démarrage, rendu de la note |

### Corrections et durcissement après le jalon v0.1

| Version | Changement |
|---|---|
| v0.1.1 | Erreurs stables et `405` contractuel : les codes émis sont tous approuvés au contrat |
| v0.1.2 | Tests unitaires consolidés sur les règles de gestion, sans base |
| v0.1.3 | Documentation : démarrage reproductible, relevé des 17 appels manuels |
| v0.1.4 | Le README ne promet plus de `PUT` non livrés, et documente la réinitialisation du jeu |
| v0.1.5 | La session de démonstration ne se rouvre plus toute seule après que le formateur l'a terminée ou clôturée (H1, RG15) |

## Étape 3 — Enveloppe

| Version | Changement |
|---|---|
| v0.1.6 | **Bug corrigé** : deux étudiants marquant leur présence en même temps ne perdent plus de présence. `REQUIRES_NEW` consommait deux connexions du pool par marquage ; le pool saturait, la transaction était annulée et la présence disparaissait. Reproduit puis corrigé, test de concurrence à l'appui |
| v1.0 | **Changement de besoin** de l'enveloppe de l'étape 3 : deux pairs par exercice, moyenne des deux notes, mention provisoire. Q6 annulée, RG20 et EF17 créées, migration V4 (unicité relâchée de `(exercice_id)` à `(exercice_id, relecteur_id)`), moyenne calculée par l'API. **Livré et vérifié** : le blocage H2 a été levé en recréant la table, les identifiants n'étant pas recopiés |

## Étape 4 — Version finale

| Version | Changement |
|---|---|
| v1.0 | Frontend : la note retenue, sa mention provisoire et le nombre de pairs reçus s'affichent ; le remplacement du lien (EF9) et la correction de note (EF10) sont exposés ; le modèle `ExerciceSession` est remis au niveau de l'API après l'enveloppe |
| v1.0 | `CHANGELOG.md` écrit depuis l'historique Git et non depuis un récit |
| v1.0 | Journal complété pour les étapes 3, 4 et 6, dont la question imposée sur le périmètre sacrifié |
| v1.0 | Backlog trié, issues fermées, dépôt public vérifié en navigation privée |

## Limites connues de la v1.0

- **La correction d'une note (EF10) n'est offerte que dans la visite qui suit le rendu.** Aucune
  opération du contrat ne liste les relectures déjà rendues par un relecteur donné ; en inventer
  une sortait du contrat. La limite est affichée dans l'écran, pas cachée.
- Le jeu de démonstration est **antérieur** au changement de l'enveloppe : chaque exercice du seed
  ne porte qu'une affectation, donc ses notes sont provisoires. C'est voulu, et c'est l'état « un
  seul pair a rendu » qu'un correcteur doit pouvoir observer.
- Le blocage anti-devinette s'applique par `etudiantId` et non par appareil, faute d'authentification
  (Q1). C'est documenté en H4.
- L'étape 5 (épreuve Git) a été annulée : un seul dépôt public existe.
