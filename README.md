# KFOKAM48

Application de présence et de relecture entre étudiants, avec backend Spring Boot et frontend Angular.

## Démarrage en trois commandes

Pré-requis : Docker Compose, Java 17+ et Node.js compatible avec Angular 22.

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

- Backend : `./backend/mvnw clean verify`
- Tests unitaires backend : `./backend/mvnw test`
- Frontend : `cd frontend && npm ci && npm run build`
- Tests frontend : `cd frontend && CI=true npx ng test --watch=false`

Angular est choisi pour ses trois écrans, ses services dédiés, ses formulaires réactifs et son routage. La moyenne affichée provient de l’API ; elle n’est pas recalculée dans le navigateur.

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

Le seed Flyway est chargé au démarrage. Il expose les identifiants stables suivants :

- code de session : `DEMO2026` ;
- promotion : `promotionId=1` ;
- étudiants de démonstration : `etudiantId=1`, `2` et `3`.

## API

Les réponses d’erreur utilisent `{code,message}`. Les 17 chemins et 18 opérations, avec leurs statuts, sont définis dans `api/contrat.yaml`.

| Méthode | Chemin | Succès |
|---|---|---|
| `POST` | `/api/sessions` | `201` |
| `POST` | `/api/presences` | `201` |
| `POST` | `/api/exercices` | `201` |
| `POST` | `/api/relectures/{id}` | `200` |
| `PUT` | `/api/relectures/{id}` | `200` |
| `GET` | `/api/tableau?promotionId=` | `200` |
| `GET` | `/api/promotions` | `200` |
| `GET` | `/api/etudiants?promotionId=` | `200` |
| `GET` | `/api/sessions/{id}` | `200` |
| `POST` | `/api/sessions/{id}/fin` | `200` |
| `POST` | `/api/sessions/{id}/cloture` | `200` |
| `GET` | `/api/sessions/{id}/presences` | `200` |
| `GET` | `/api/sessions/{id}/exercices` | `200` |
| `POST` | `/api/presences/manuelle` | `201` |
| `POST` | `/api/relectures/{id}/debut` | `200` |
| `PUT` | `/api/exercices/{id}` | `200` |
| `GET` | `/api/etudiants/{etudiantId}/relectures-recues` | `200` |
| `GET` | `/api/relectures/a-faire?etudiantId=` | `200` |

## Structure

- `backend/` : API Spring Boot, migrations Flyway et tests ;
- `frontend/` : application Angular et tests Vitest ;
- `api/contrat.yaml` : contrat HTTP ;
- `docs/` : cahier des charges, backlog, diagrammes et journal ;
- `docker-compose.yml` : PostgreSQL 16 et backend.
