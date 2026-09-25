# Appels manuels du contrat — preuve de la consigne 14.5

> **Ce que c'est.** Le relevé **verbatim** des 17 appels manuels exigés par la consigne 14.5
> (« Conserver les commandes, statuts et corps JSON observés pour le résumé final. Ne pas
> déclarer le jalon avant ces appels. »), joué contre l'application démarrée par
> `docker compose up --build` (backend en conteneur, branché sur PostgreSQL, Flyway V1→V3).
>
> **Ce n'est pas une sortie de test automatisé.** C'est un appel `curl` par point. Les tests
> d'intégration existent par ailleurs (`./backend/mvnw clean verify`) ; ce document ne s'y
> substitue pas, il prouve que le démarrage documenté et le contrat se rencontrent réellement.

## Comment reproduire ce relevé

```bash
docker compose up -d --build      # terminal 1, depuis la racine du dépôt
bash docs/appels-manuels.sh       # terminal 2 — renvoie le relevé sur la sortie standard
```

Le script crée **ses propres** sessions : il est rejouable autant de fois que voulu et ne dépend
d'aucun identifiant figé, hormis `promotionId=1` et les étudiants `1` et `2`, garantis par les
migrations de seed. Il n'a besoin que de `curl` et de `jq`.

Pour repartir d'un jeu de démonstration intact avant de rejouer :

```bash
docker compose down -v && docker compose up -d --build
```

## Environnement du relevé

| Élément | Valeur observée |
|---|---|
| Base de données | PostgreSQL 16 en conteneur, profil Spring `docker`, Flyway V1→V3 |
| Backend | `http://localhost:8080` (image construite par `docker compose up --build`) |
| Méthode | `curl` avec `-w "%{http_code}"` ; corps ré-indentés par `jq` pour la lecture |

> Les octets ci-dessous sont ceux reçus de l'API ; seule l'indentation de lecture est ajoutée par
> `jq`. **Aucune valeur, aucun statut n'est inventé ou reconstitué.**

## Résultat des 17 points de la consigne 14.5

| # | Point vérifié | Appel | Statut observé |
|---:|---|---|---|
| 1 | Promotions et étudiants de référence | `GET /api/promotions` · `GET /api/etudiants?promotionId=1` | `200` · `200` (60 étudiants) |
| 2 | Ouverture d'une session (code expirant) | `POST /api/sessions` | `201` |
| 3 | Marquer sa présence | `POST /api/presences` | `201` |
| 4 | Doublon de présence | `POST /api/presences` (bis) | `409 DEJA_PRESENT` |
| 5 | Détail des présences | `GET /api/sessions/{id}/presences` | `200` |
| 6 | Dépôt d'un exercice | `POST /api/exercices` | `201` (`EN_ATTENTE_DE_RELECTURE`) |
| 7 | Détail des exercices / affectation | `GET /api/sessions/{id}/exercices` | `200` (`relecteurId=2`) |
| 8 | Relectures à faire (sans identité d'auteur) | `GET /api/relectures/a-faire?etudiantId=2` | `200` |
| 9 | Démarrer une relecture | `POST /api/relectures/{id}/debut` | `200` |
| 10 | Rendu de la note, puis second rendu refusé | `POST /api/relectures/{id}` | `200`, puis `409 RELECTURE_DEJA_RENDUE` |
| 11 | Résultats reçus (anonymat du relecteur) | `GET /api/etudiants/1/relectures-recues` | `200` (aucun `relecteurId`, aucun nom) |
| 12 | Tableau par promotion | `GET /api/tableau?promotionId=1` | `200` |
| 13 | Moyenne fournie par l'API, non recalculée | (lecture de la ligne `etudiantId=1`) | `moyenne=16.0` |
| 14 | Terminer la session | `POST /api/sessions/{id}/fin` | `200` |
| 15 | Clôturer la session | `POST /api/sessions/{id}/cloture` | `200` |
| 16 | Écriture refusée après clôture | `POST /api/presences` | `410 SESSION_CLOTUREE` |
| 17 | EF9 remplacement du lien · EF10 correction de note *(Should)* | `PUT /api/exercices/{id}` · `PUT /api/relectures/{id}` | `200` puis verrou `409 RELECTURE_COMMENCEE` · `200` |

Les deux opérations `PUT` (EF9/EF10) sont des **Should** livrées après le jalon `[JALON] v0.1`
(issue #14 close, PR #51) ; elles sont signalées comme telles dans le `README` et exercées ici.

## Relevé brut (sortie de `bash docs/appels-manuels.sh`)

