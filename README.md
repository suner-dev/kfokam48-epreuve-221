# KFOKAM48

Application de présence et de relecture entre étudiants, avec backend Spring Boot et frontend Angular.

## Démarrage en trois commandes

Pré-requis : Docker Compose, Java 17+ et Node.js compatible avec Angular 22.

1. Créer le fichier local sans secret commité :

   ```bash
   printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 16)" > .env
   printf 'POSTGRES_USER=kfokam48\nPOSTGRES_DATABASE=kfokam48\n' >> .env
   ```

2. Démarrer PostgreSQL 16 et le backend :

   ```bash
   docker compose up -d --build
   ```

3. Démarrer le frontend :

   ```bash
   cd frontend && npm ci && npm start
   ```

Le backend répond sur `http://localhost:8080`. Le frontend Angular répond sur `http://localhost:4200` et utilise le proxy configuré dans `frontend/proxy.conf.json`.

## Tests et build

- Backend : `./backend/mvnw clean verify`
- Tests unitaires backend : `./backend/mvnw test`
- Frontend : `cd frontend && npm ci && npm run build`
- Tests frontend : `cd frontend && CI=true npx ng test --watch=false`

Angular est choisi pour ses trois écrans, ses services dédiés, ses formulaires réactifs et son routage. La moyenne affichée provient de l’API ; elle n’est pas recalculée dans le navigateur.

## Configuration

Les variables suivantes sont lues par Docker Compose et le backend :

- `POSTGRES_USER` : utilisateur PostgreSQL, valeur locale documentée `kfokam48` ;
- `POSTGRES_DATABASE` : base PostgreSQL, valeur locale documentée `kfokam48` ;
- `POSTGRES_PASSWORD` : mot de passe local, obligatoire et jamais commité ;
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` : variables équivalentes pour le backend exécuté hors Compose.

Le fichier `.env` est ignoré par Git. `.env.example` ne contient aucun mot de passe.

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
