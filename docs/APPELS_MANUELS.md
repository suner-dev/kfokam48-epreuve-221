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


```text
# Rejeu des appels manuels du contrat — consigne 14.5
# API      : http://localhost:8080
# Date     : 2026-09-25T16:59:32Z
# Commit   : c91ecddcda44b6d68cbfa99d6812647b4d742b67

### 1a. Lister les promotions
$ curl -s -X GET 'http://localhost:8080/api/promotions'
< HTTP 200
< [{"id":1,"nom":"Promotion Démo KFOKAM48"}]

### 1b. Lister les etudiants de la promotion 1 (extrait)
$ curl -s -X GET 'http://localhost:8080/api/etudiants?promotionId=1'
< HTTP 200
< [{"id":4,"nom":"Demo","prenom":"Etudiant1"},{"id":13,"nom":"Demo","prenom":"Etudiant10"},{"id":14,"nom":"Demo","prenom":"Etudiant11"},{"id":15,"nom":"Demo","prenom":"Etudiant12"},{"id":16,"nom":"Demo","prenom":"Etudiant13"},{"id":17,"nom":"Demo","prenom":"Etudiant14"},{"id":18,"nom":"Demo","prenom":"Etudiant15"},{"id":19,"nom":"Demo","prenom":"Etudiant16"},{"id":20,"nom":"Demo","prenom":"Etudiant17"},{"id":21,"nom":"Demo","prenom":"Etudiant18"},{"id":22,"nom":"Demo","prenom":"Etudiant19"},{"id":5,"nom":"Demo","prenom":"Etudiant2"},{"id":23,"nom":"Demo","prenom":"Etudiant20"},{"id":24,"nom":"Demo","prenom":"Etudiant21"},{"id":25,"nom":"Demo","prenom":"Etudiant22"},{"id":26,"nom":"Demo","prenom":"Etudiant23"},{"id":27,"nom":"Demo","prenom":"Etudiant24"},{"id":28,"nom":"Demo","prenom":"Etudiant25"},{"id":29,"nom":"Demo","prenom":"Etudiant26"},{"id":30,"nom":"Demo","prenom":"Etudiant27"},{"id":31,"nom":"Demo","prenom":"Etudiant28"},{"id":32,"nom":"Demo","prenom":"Etudiant29"},{"id":6,"nom":"Demo","prenom":"Etudiant3"},{"id":33,"nom":"Demo","prenom":"Etudiant30"},{"id":34,"nom":"Demo","prenom":"Etudiant31"},{"id":35,"nom":"Demo","prenom":"Etudiant32"},{"id":36,"nom":"Demo","prenom":"Etudiant33"},{"id":37,"nom":"Demo","prenom":"Etudiant34"},{"id":38,"nom":"Demo","prenom":"Etudiant35"},{"id":39,"nom":"Demo","prenom":"Etudiant36"},{"id":40,"nom":"Demo","prenom":"Etudiant37"},{"id":41,"nom":"Demo","prenom":"Etudiant38"},{"id":42,"nom":"Demo","prenom":"Etudiant39"},{"id":7,"nom":"Demo","prenom":"Etudiant4"},{"id":43,"nom":"Demo","prenom":"Etudiant40"},{"id":44,"nom":"Demo","prenom":"Etudiant41"},{"id":45,"nom":"Demo","prenom":"Etudiant42"},{"id":46,"nom":"Demo","prenom":"Etudiant43"},{"id":47,"nom":"Demo","prenom":"Etudiant44"},{"id":48,"nom":"Demo","prenom":"Etudiant45"},{"id":49,"nom":"Demo","prenom":"Etudiant46"},{"id":50,"nom":"Demo","prenom":"Etudiant47"},{"id":51,"nom":"Demo","prenom":"Etudiant48"},{"id":52,"nom":"Demo","prenom":"Etudiant49"},{"id":8,"nom":"Demo","prenom":"Etudiant5"},{"id":53,"nom":"Demo","prenom":"Etudiant50"},{"id":54,"nom":"Demo","prenom":"Etudiant51"},{"id":55,"nom":"Demo","prenom":"Etudiant52"},{"id":56,"nom":"Demo","prenom":"Etudiant53"},{"id":57,"nom":"Demo","prenom":"Etudiant54"},{"id":58,"nom":"Demo","prenom":"Etudiant55"},{"id":59,"nom":"Demo","prenom":"Etudiant56"},{"id":60,"nom":"Demo","prenom":"Etudiant57"},{"id":9,"nom":"Demo","prenom":"Etudiant6"},{"id":10,"nom":"Demo","prenom":"Etudiant7"},{"id":11,"nom":"Demo","prenom":"Etudiant8"},{"id":12,"nom":"Demo","prenom":"Etudiant9"},{"id":1,"nom":"Dupont","prenom":"Alice"},{"id":2,"nom":"Martin","prenom":"Bruno"},{"id":3,"nom":"Nkoa","prenom":"Carla"}]

-> 60 etudiants renvoies pour la promotion 1

### 2. Ouvrir une session
$ curl -s -X POST 'http://localhost:8080/api/sessions' -H 'Content-Type: application/json' -d '{"titre":"Session Appels Manuels","promotionId":1}'
< HTTP 201
< {"id":3,"code":"VKQWPELD","ouvertureAt":"2026-09-25T16:59:32.094223473Z","expirationAt":"2026-09-25T17:14:32.094223473Z"}

### 3. Marquer la presence de l'etudiant 1
$ curl -s -X POST 'http://localhost:8080/api/presences' -H 'Content-Type: application/json' -d '{"code":"VKQWPELD","etudiantId":1}'
< HTTP 201
< {"id":61,"sessionId":3,"etudiantId":1,"source":"ETUDIANT"}

### 4.presence en double (attendu 409 DEJA_PRESENT)
$ curl -s -X POST 'http://localhost:8080/api/presences' -H 'Content-Type: application/json' -d '{"code":"VKQWPELD","etudiantId":1}'
< HTTP 409
< {"code":"DEJA_PRESENT","message":"La présence est déjà enregistrée pour cette session."}

### 5. Detail des presences de la session
$ curl -s -X GET 'http://localhost:8080/api/sessions/3/presences'
< HTTP 200
< [{"etudiantId":4,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":13,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":14,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":15,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":16,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":17,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":18,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":19,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":20,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":21,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":22,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":5,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":23,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":24,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":25,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":26,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":27,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":28,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":29,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":30,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":31,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":32,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":6,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":33,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":34,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":35,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":36,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":37,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":38,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":39,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":40,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":41,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":42,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":7,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":43,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":44,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":45,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":46,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":47,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":48,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":49,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":50,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":51,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":52,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":8,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":53,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":54,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":55,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":56,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":57,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":58,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":59,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":60,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":9,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":10,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":11,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":12,"nom":"Demo","present":false,"source":null,"marqueeAt":null},{"etudiantId":1,"nom":"Dupont","present":true,"source":"ETUDIANT","marqueeAt":"2026-09-25T16:59:32.265570Z"},{"etudiantId":2,"nom":"Martin","present":false,"source":null,"marqueeAt":null},{"etudiantId":3,"nom":"Nkoa","present":false,"source":null,"marqueeAt":null}]

### 6a. Presence de l'etudiant 2 (pour l'appairage)
$ curl -s -X POST 'http://localhost:8080/api/presences' -H 'Content-Type: application/json' -d '{"code":"VKQWPELD","etudiantId":2}'
< HTTP 201
< {"id":62,"sessionId":3,"etudiantId":2,"source":"ETUDIANT"}

### 6b. Depot du lien de l'etudiant 1
$ curl -s -X POST 'http://localhost:8080/api/exercices' -H 'Content-Type: application/json' -d '{"sessionId":3,"etudiantId":1,"lien":"https://example.test/manuels/ex1"}'
< HTTP 201
< {"id":5,"statut":"EN_ATTENTE_DE_RELECTURE"}

### 7. Detail des exercices de la session
$ curl -s -X GET 'http://localhost:8080/api/sessions/3/exercices'
< HTTP 200
< [{"id":5,"etudiantId":1,"statut":"EN_ATTENTE_DE_RELECTURE","relecteurId":2,"relectureId":5,"commenceeAt":null,"rendueAt":null}]

### 8. Relectures a faire pour l'etudiant 2
$ curl -s -X GET 'http://localhost:8080/api/relectures/a-faire?etudiantId=2'
< HTTP 200
< [{"relectureId":1,"exerciceId":1,"lienExercice":"https://example.test/exercices/demo-alice","commenceeAt":null},{"relectureId":5,"exerciceId":5,"lienExercice":"https://example.test/manuels/ex1","commenceeAt":null}]

### 9a. EF9 — Remplacement du lien avant debut (200 attendu)
$ curl -s -X PUT 'http://localhost:8080/api/exercices/5' -H 'Content-Type: application/json' -d '{"lien":"https://example.test/manuels/ex1-v2"}'
< HTTP 200
< {"id":5,"statut":"EN_ATTENTE_DE_RELECTURE"}

### 9b. Demarrer la relecture
$ curl -s -X POST 'http://localhost:8080/api/relectures/5/debut' -H 'Content-Type: application/json' -d '{}'
< HTTP 200
< {"id":5,"commenceeAt":"2026-09-25T16:59:33.717008055Z"}

### 9c. EF9 verrou — remplacement apres debut (attendu 409 RELECTURE_COMMENCEE)
$ curl -s -X PUT 'http://localhost:8080/api/exercices/5' -H 'Content-Type: application/json' -d '{"lien":"https://example.test/manuels/ex1-v3"}'
< HTTP 409
< {"code":"RELECTURE_COMMENCEE","message":"La relecture a commencé."}

### 10a. Rendu de la note (15)
$ curl -s -X POST 'http://localhost:8080/api/relectures/5' -H 'Content-Type: application/json' -d '{"note":15,"commentaire":"Bon travail."}'
< HTTP 200
< {"id":5,"note":15,"commentaire":"Bon travail.","rendueAt":"2026-09-25T16:59:34.021645242Z"}

### 10b. Second rendu (attendu 409 RELECTURE_DEJA_RENDUE)
$ curl -s -X POST 'http://localhost:8080/api/relectures/5' -H 'Content-Type: application/json' -d '{"note":12,"commentaire":"Encore."}'
< HTTP 409
< {"code":"RELECTURE_DEJA_RENDUE","message":"La relecture a déjà été rendue."}

### 10c. EF10 — Correction de la note avant cloture (200 attendu)
$ curl -s -X PUT 'http://localhost:8080/api/relectures/5' -H 'Content-Type: application/json' -d '{"note":17,"commentaire":"Apres relecture attentive."}'
< HTTP 200
< {"id":5,"note":17,"commentaire":"Apres relecture attentive.","rendueAt":"2026-09-25T16:59:34.288130442Z"}

### 11. Resultats recus par l'etudiant 1 (anonymat du relecteur)
$ curl -s -X GET 'http://localhost:8080/api/etudiants/1/relectures-recues'
< HTTP 200
< [{"exerciceId":5,"sessionId":3,"lienExercice":"https://example.test/manuels/ex1-v2","note":17,"commentaire":"Apres relecture attentive."},{"exerciceId":2,"sessionId":2,"lienExercice":"https://example.test/exercices/demo-relu","note":15,"commentaire":"Relecture relue de démonstration"}]

### 12. Tableau de la promotion 1
$ curl -s -X GET 'http://localhost:8080/api/tableau?promotionId=1'
< HTTP 200
< [{"etudiantId":4,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":13,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":14,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":15,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":16,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":17,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":18,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":19,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":20,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":21,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":22,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":5,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":23,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":24,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":25,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":26,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":27,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":28,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":29,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":30,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":31,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":32,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":6,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":33,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":34,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":35,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":36,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":37,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":38,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":39,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":40,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":41,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":42,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":7,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":43,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":44,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":45,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":46,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":47,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":48,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":49,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":50,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":51,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":52,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":8,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":53,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":54,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":55,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":56,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":57,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":58,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":59,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":60,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":9,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":10,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":11,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":12,"nom":"Demo","presences":1,"exercicesDeposes":0,"moyenne":null,"relecturesEnAttente":0},{"etudiantId":1,"nom":"Dupont","presences":2,"exercicesDeposes":3,"moyenne":16.0,"relecturesEnAttente":0},{"etudiantId":2,"nom":"Martin","presences":2,"exercicesDeposes":1,"moyenne":null,"relecturesEnAttente":1},{"etudiantId":3,"nom":"Nkoa","presences":1,"exercicesDeposes":1,"moyenne":null,"relecturesEnAttente":1}]

-> 13. La moyenne de l'etudiant 1 ci-dessus est fournie par l'API (note 17 recue) ;
        le frontend l'affiche sans jamais la recalculer (contrainte F3).

### 14. Terminer la session
$ curl -s -X POST 'http://localhost:8080/api/sessions/3/fin' -H 'Content-Type: application/json' -d '{}'
< HTTP 200
< {"id":3,"finAt":"2026-09-25T16:59:34.632634776Z"}

### 15. Cloturer la session
$ curl -s -X POST 'http://localhost:8080/api/sessions/3/cloture' -H 'Content-Type: application/json' -d '{}'
< HTTP 200
< {"id":3,"finAt":"2026-09-25T16:59:34.632635Z","clotureAt":"2026-09-25T16:59:35.177599201Z"}

### 16. Presence apres cloture (attendu 410 SESSION_CLOTUREE)
$ curl -s -X POST 'http://localhost:8080/api/presences' -H 'Content-Type: application/json' -d '{"code":"VKQWPELD","etudiantId":3}'
< HTTP 410
< {"code":"SESSION_CLOTUREE","message":"La session est clôturée."}

### Rejeu termine. Session id=3 code=VKQWPELD exercice=5 relecture=5.
```
