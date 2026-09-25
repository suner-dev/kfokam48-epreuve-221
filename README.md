# KFOKAM48

Application de présence et de relecture entre étudiants, avec backend Spring Boot et frontend Angular.

## Démarrage en trois commandes

### Pré-requis, selon ce que vous lancez

| Vous lancez | Il vous faut | Testé sur |
|---|---|---|
| Le backend (Docker) | **Docker + Docker Compose.** Rien d'autre : la compilation Maven a lieu dans l'image, ni Java ni Maven ne sont requis sur la machine. | Docker 29.7 |
| Le frontend | **Node.js 22.12+** et `npm`. | Node 22.23.2 / npm 12.0.2 |
| Les tests backend hors Docker | **Java 17+**. Le wrapper `mvnw` est commité, Maven n'est pas à installer. | OpenJDK 26.0.2 (cible de compilation : Java 17) |

### Les trois commandes

1. Démarrer PostgreSQL 16 et le backend :

   ```bash
   docker compose up -d --build
   ```

   Aucun fichier `.env` n'est nécessaire : les valeurs par défaut sont des valeurs de
   démonstration (voir [Configuration](#configuration)).

2. Démarrer le frontend :

   ```bash
   cd frontend && npm ci && npm start
   ```

Le backend répond sur `http://localhost:8080`. Le frontend Angular répond sur `http://localhost:4200` et utilise le proxy configuré dans `frontend/proxy.conf.json`.

Vérification du démarrage avant d'ouvrir un écran :

```bash
curl -s http://localhost:8080/api/promotions
# 200, [{"id":1,"nom":"Promotion Démo KFOKAM48"}]
```

## Tests et build

- Backend : `./backend/mvnw clean verify` — 35 tests unitaires et 37 tests d'intégration,
  tous sur H2, sans aucune base locale à installer.
- Tests unitaires backend : `./backend/mvnw test`
- Frontend : `cd frontend && npm ci && npm run build`
- Tests frontend : `cd frontend && CI=true npx ng test --watch=false` — 13 fichiers, 47 tests
- Appels manuels du contrat : `bash docs/appels-manuels.sh` — rejoue les 17 appels de la
  consigne 14.5 et affiche commande, statut et corps. Le relevé verbatim du dernier passage
  est dans [`docs/APPELS_MANUELS.md`](docs/APPELS_MANUELS.md).

Angular est choisi pour ses trois écrans, ses services dédiés, ses formulaires réactifs et son routage. La moyenne affichée provient de l’API ; elle n’est pas recalculée dans le navigateur.

**Chaque exercice est relu par deux pairs.** La note retenue est la moyenne des notes rendues, et elle est marquée **provistoire** tant que les deux pairs n'ont pas tous deux rendu — c'est la seule situation où l'API renvoie une note non entière. L'écran étudiant affiche cette mention et le nombre de notes reçues sur les deux attendues ; la moyenne, elle, ne sort jamais du calcul de l'API.

## Configuration

La configuration est séparée en trois fichiers, comme l'exige la consigne 2.2 ; les
migrations Flyway sont compatibles avec PostgreSQL et H2 :

| Fichier | Profil | Base | Usage |
|---|---|---|---|
| `application.yml` | *(aucun)* | `jdbc:postgresql://localhost:5432/kfokam48` | valeurs par défaut sans secret, lancement hors conteneur |
| `application-docker.yml` | `docker` | `jdbc:postgresql://postgres:5432/kfokam48` | service `backend` de `docker-compose.yml` |
| `application-test.yml` | `test` | H2 en mémoire, mode PostgreSQL | **tous** les tests automatisés, aucune base locale requise |

Le conteneur démarre avec `SPRING_PROFILES_ACTIVE=docker`. Les variables lues :

- `POSTGRES_USER`, `POSTGRES_DATABASE`, `POSTGRES_PASSWORD` : utilisées par Docker Compose,
  valeur de démonstration `kfokam48` pour chacune ;
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE` : les mêmes côté backend, pour que
  le conteneur comme un processus local se configurent sans modifier aucun fichier.

Aucune de ces valeurs n'est un secret réel. Le fichier `.env`, s'il existe, est ignoré par
Git ; `.env.example` documente les mêmes valeurs de démonstration et reste optionnel.
Le schéma est porté par Flyway et `spring.jpa.hibernate.ddl-auto=validate` : l'application
refuse de démarrer si le schéma et les entités divergent, elle ne modifie jamais la base elle-même.

## Données de démonstration

Le seed est porté par les migrations Flyway `V2` et `V3`, appliquées au premier démarrage sur une
base neuve : 60 étudiants, 2 sessions, 60 présences et 4 exercices. Rien n'est secret, rien ne
dépend d'un utilisateur réel, et le jeu est déterministe.

**Une promotion** — `promotionId=1`, `Promotion Démo KFOKAM48`.

**60 étudiants aux identifiants stables :**

| `etudiantId` | Nom | Prénom | Rôle dans le jeu |
|---|---|---|---|
| `1` | Dupont | Alice | autrice du RELU et d'un EN_ATTENTE |
| `2` | Martin | Bruno | relecteur du RELU, autrice d'un EN_ATTENTE |
| `3` | Nkoa | Carla | autrice de l'exercice sans relecteur |
| `4` à `60` | Demo | Etudiant1 à Etudiant57 | les 57 autres, présents à `DEMO-B` |

**Deux sessions :**

| `id` | Code | Fenêtre | Contenu |
|---|---|---|---|
| `1` | `DEMO2026` | rafraîchie à chaque démarrage **si la session est encore ouverte** : `expirationAt = maintenant + 15 min` | l'exercice `1`, `EN_ATTENTE_DE_RELECTURE`, relu par l'étudiant `2` |
| `2` | `DEMO-B` | 24 h après le seed | les 57 présences `Demo` (alternées `ETUDIANT` / `FORMATEUR`) et les 3 exercices ci-dessous |

**Les trois états d'exercice, pour que le tableau ait de quoi montrer.** Le seed est antérieur au changement de l'enveloppe : chaque exercice y porte une seule affectation, ce qui rend ses notes provisoires — c'est voulu, et c'est exactement l'état « un seul pair a rendu » qu'un correcteur doit voir.

| `exerciceId` | Session | Auteur | `statut` | Relecteur |
|---:|---:|---:|---|---|
| `2` | `DEMO-B` | `1` | `RELU` (note 15) | `2` |
| `3` | `DEMO-B` | `2` | `EN_ATTENTE_DE_RELECTURE` | `3` |
| `4` | `DEMO-B` | `3` | `EN_ATTENTE_SANS_RELECTEUR` | `null` — cas H3, seul cas possible ici |

> `DEMO2026` est la session à utiliser pour jouer le parcours étudiant : son code est connu et
> sa fenêtre est renouvelée à chaque démarrage. Si vous la terminez ou la clôturez, elle le
> reste au redémarrage suivant : la lecture seule promise par la clôture est tenue.
>
> Les étudiants `1`, `2` et `3` ne sont pas comptés présents à `DEMO-B`, le seed ne couvrant que
> les 57 étudiants `Demo`. C'est volontaire : c'est ce qui rend les trois états d'exercice
> visibles sans qu'il faille créer une présence. Le tableau reste exact.

### Réinitialiser le jeu de démonstration

Le jeu se modifie quand vous jouez : une session créée, un exercice déposé, une note rendue.
Pour repartir de l'état décrit ci-dessus :

```bash
docker compose down -v && docker compose up -d
```

`down -v` supprime le volume PostgreSQL named ; `up` rejoue alors les migrations `V1`→`V3` sur une
base neuve. Sans le `-v`, le volume est conservé et le jeu reste tel que vous l'avez laissé.

## API

Les réponses d’erreur utilisent `{code,message}`. Les 17 chemins et 18 opérations, avec leurs statuts, sont définis dans `api/contrat.yaml`. Les 18 sont livrées et chacune a été rejouée manuellement : voir [`docs/APPELS_MANUELS.md`](docs/APPELS_MANUELS.md).

| Méthode | Chemin | Succès | Ticket |
|---|---|---|---|
| `POST` | `/api/sessions` | `201` | EF1 · imposé |
| `POST` | `/api/presences` | `201` | EF2 · imposé |
| `POST` | `/api/exercices` | `201` | EF3 · imposé |
| `POST` | `/api/relectures/{id}` | `200` | EF5 · imposé |
| `GET` | `/api/tableau?promotionId=` | `200` | EF6 · imposé |
| `GET` | `/api/promotions` | `200` | EF13 |
| `GET` | `/api/etudiants?promotionId=` | `200` | EF13 |
| `GET` | `/api/sessions/{id}` | `200` | EF1 |
| `POST` | `/api/sessions/{id}/fin` | `200` | EF15 |
| `POST` | `/api/sessions/{id}/cloture` | `200` | EF15 · trou H1 |
| `GET` | `/api/sessions/{id}/presences` | `200` | EF8 · Q14 |
| `GET` | `/api/sessions/{id}/exercices` | `200` | EF4 · Q11 |
| `POST` | `/api/presences/manuelle` | `201` | EF8 · Q14 |
| `POST` | `/api/relectures/{id}/debut` | `200` | EF16 |
| `GET` | `/api/etudiants/{etudiantId}/relectures-recues` | `200` | EF14 · Q8 |
| `GET` | `/api/relectures/a-faire?etudiantId=` | `200` | EF16 |
| `PUT` | `/api/relectures/{id}` | `200` | EF10 · Should, issue #14, **après le jalon v0.1** |
| `PUT` | `/api/exercices/{id}` | `200` | EF9 · Should, issue #14, **après le jalon v0.1** |

Les deux `PUT` sont les seules opérations ajoutées après `[JALON] v0.1` : elles matérialisent
l'arbitrage Q10 (relecture modifiable tant que la session n'est pas clôturée) et Q13 (lien
remplaçable tant que la relecture n'a pas commencé). Elles sont dans le contrat depuis l'étape
d'analyse ; elles sont implémentées uniquement depuis l'issue #14.

## Structure

- `backend/` : API Spring Boot, migrations Flyway et tests ;
- `frontend/` : application Angular et tests Vitest ;
- `api/contrat.yaml` : contrat HTTP ;
- `docs/` : cahier des charges, backlog, diagrammes, journal, et les appels manuels
  (`APPELS_MANUELS.md` + `appels-manuels.sh`) ;
- `docker-compose.yml` : PostgreSQL 16 et backend.
