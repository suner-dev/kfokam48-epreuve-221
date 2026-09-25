# Backlog — issues à créer sur `github.com/suner-dev/kfokam48-epreuve-221`

> Chaque issue : **titre = un résultat** (pas une tâche technique), **critères d'acceptation
> vérifiables**, **priorité Must/Should/Could**, **renvoi `EFx`/`RGx`**. À créer via
> `gh issue create` (ou l'UI) **pendant l'Étape 1**, avant `[JALON] analyse`.
> Règle d'or du sujet : **une branche par ticket, une PR par branche, issues fermées par les
> commits** (`Closes #n`). **Zéro issue = −10 pts** ; ce backlog contient **16 tickets spécifiés**,
> dont les issues GitHub restent à créer et à lier aux commits.

## Stories MUST (livrées en v0.1 — Étape 2)

### #1 — En tant qu'étudiant, je marque ma présence avec le code de la session
- **Renvois :** EF2 · RG1 · RG2 · RG3 · D3
- **Critères :**
  - `POST /api/presences {code, etudiantId}` valide, avant expiration et avant `finAt` → `201 {id, sessionId, etudiantId, source:"ETUDIANT"}`.
  - Code inconnu → `400 CODE_INCONNU` ; code expiré (`now >= expirationAt`) ou session terminée → `410 CODE_EXPIRE`/`410 SESSION_TERMINEE` ; déjà présent → `409 DEJA_PRESENT`.
  - Tout message d'erreur au format `{code, message}` (**B4**, jamais de stack trace).
- **Test :** intégration `POST /api/presences` (201 + 410 + 409).

### #2 — En tant que formateur, j'ouvre une session et j'obtiens un code qui expire
- **Renvois :** EF1 · RG1
- **Critères :** `POST /api/sessions {titre, promotionId}` → `201 {id, code, ouvertureAt, expirationAt}` avec `expirationAt = ouvertureAt + 15 min` ; champ manquant → `400`.

### #3 — En tant qu'étudiant, je dépose le lien de mon exercice pour une session
- **Renvois :** EF3 · RG11 · RG16 · D4
- **Critères :** `POST /api/exercices {sessionId, etudiantId, lien}` (URI) avant clôture → `201 {id, statut}` avec une affectation unique ; lien invalide → `400 LIEN_INVALIDE` ; déjà déposé → `409 EXERCICE_DEJA_DEPOSE` ; après clôture → `410 SESSION_CLOTUREE`. Le dépôt reste permis après `finAt` jusqu'à `clotureAt` (Q12).

### #4 — En tant que système, j'assigne automatiquement un relecteur présent à chaque exercice
- **Renvois :** EF4 · EF7 · RG5 · RG6 · RG7 · H3
- **Critères :** au dépôt, si au moins un étudiant **présent à la session** est différent de l'auteur, tirage **aléatoire** ; **1 seule affectation** par exercice (`relecture.exercice_id UNIQUE`) ; sinon affectation conservée **sans relecteur** (`relecteur_id NULL`) et visible via `GET /api/sessions/{id}/exercices` (Q11). Une nouvelle présence déclenche la réévaluation des exercices sans pair.

### #5 — En tant que relecteur, je rends une note (0–20) et un commentaire
- **Renvois :** EF5 · RG5 · RG8 · H2 · D2
- **Critères :** `POST /api/relectures/{id}` (id = relecture assignée) `{note∈[0,20] entier, commentaire}` → `200` ; note hors bornes/non entière → `400 NOTE_INVALIDE` ; auto-relecture → `403 AUTO_RELECTURE` ; déjà rendue → `409 RELECTURE_DEJA_RENDUE`.

### #6 — En tant que formateur, je vois le tableau par promotion
- **Renvois :** EF6 · RG10 · Q16 · F3
- **Critères :** `GET /api/tableau?promotionId=` → `200` la liste imposée `{etudiantId, nom, presences, exercicesDeposes, moyenne (nullable), relecturesEnAttente}` ; `relecturesEnAttente` compte les tâches dues à l'étudiant, tandis que `GET /api/sessions/{id}/exercices` rend visibles les exercices en attente Q11. Promotion inconnue → `404 PROMOTION_INCONNUE`. La moyenne vient de l'API (ENF7/F3).

### #7 — En tant qu'étudiant, je choisis mon nom dans une liste (sans mot de passe)
- **Renvois :** EF13 · H4 · Q1 (authentification exclue, sélection d'identité incluse)
- **Critères :** `GET /api/etudiants?promotionId=` alimente l'écran étudiant ; aucune saisie d'identifiant.

### #8 — En tant que formateur, j'ajoute une présence à la main et elle se distingue
- **Renvois :** EF8 · RG13 · H6 · Q14
- **Critères :** `POST /api/presences/manuelle {sessionId, etudiantId}` → `201 {…, source:"FORMATEUR"}` ; déjà présent → `409` ; session clôturée → `410`. `GET /api/sessions/{id}/presences` expose `source` et permet de voir l'origine formateur sans modifier le schéma imposé du tableau.

### #9 — En tant qu'auteur, je ne vois jamais l'identité de mon relecteur
- **Renvois :** EF14 · RG14 · Q8 · H9
- **Critères :** `GET /api/etudiants/{etudiantId}/relectures-recues` ne révèle ni `relecteurId` ni nom du relecteur ; l'auteur voit uniquement l'exercice, la note et le commentaire. Le DTO et la réponse JSON sont vérifiés, pas seulement l'écran.

### #10 — Écran formateur (ouvrir une session + tableau)
- **Renvois :** F2 · EF1 · EF6 · EF8 · EF11 · EF15
- **Critères :** écran Angular avec ouverture, fin/clôture, tableau agrégé, détails par session, présences source FORMATEUR et exercices Q11 ; états **chargement/erreur** gérés ; appels via la **couche `services/`** dédiée (**F3**).

### #11 — Écran étudiant (présence + dépôt de lien)
- **Renvois :** F2 · EF2 · EF3 · EF13
- **Critères :** sélection de l'étudiant, saisie du code, dépôt du lien ; messages d'erreur contractuels affichés lisiblement.

### #12 — Écran relecteur (mes relectures à faire + rendu de note)
- **Renvois :** F2 · EF4 · EF5 · EF16 · H8
- **Critères :** liste via `GET /api/relectures/a-faire?etudiantId=` sans identité de l'auteur ; démarrage via `POST /api/relectures/{id}/debut` ; formulaire note (0–20 entier) + commentaire ; validation avant envoi ; le frontend ne recalcule pas la moyenne.

## Stories SHOULD (après l'enveloppe / v1.0 si le temps le permet)

> Cette section contient aussi #13 et #14 : #13 est une dépendance **Must** de Q10/Q12, et #14
> contient EF16 (**Must**) avec EF9/EF10 (Should). Les échéances déclarées priment sur l'ordre de
> présentation.

### #13 — En tant que formateur, je clôture une session (elle devient lecture seule)
- **Priorité : Must (dépendance Q10/Q12)** · **Renvois :** EF11 · EF15 · RG9 · RG15 · **H1/H7**
- **Critères :** `POST /api/sessions/{id}/fin` fixe `finAt` ; `POST /api/sessions/{id}/cloture` fixe `clotureAt` (et `finAt` si nécessaire) ; déjà clôturée → `409 SESSION_DEJA_CLOTUREE` ; après clôture, dépôt, présence, remplacement, rendu et correction sont refusés selon leur code d'erreur.

### #14 — Je corrige une relecture et je remplace un lien avant l'échéance appropriate
- **Renvois :** EF9 · EF10 · EF16 · RG9 · RG12 · RG18 · arbitrage **Q10** · **H8**
- **Critères :** `POST /api/relectures/{id}/debut` ou la première soumission de note fixe `commenceeAt` ; `PUT /api/relectures/{id}` est autorisé tant que `clotureAt IS NULL` et l'exercice reste `RELU` ; `PUT /api/exercices/{id}` est autorisé seulement tant que `commenceeAt IS NULL` (`409 RELECTURE_COMMENCEE` après démarrage).

## Stories COULD (si temps restant — backlog trié, non bloquant)

### #15 — Anti-devinette : blocage 2 min après 5 saisies invalides
- **Priorité : Should** · **Renvois :** EF12 · RG4 · **H4/H5** · table `code_tentative` (D2)
- **Critères :** les cinq premières saisies invalides sous un `etudiantId` déclaré sont comptées globalement ; à partir de la cinquième, les nouvelles tentatives renvoient `400 TROP_ESSAIS` pendant 120 s. Un succès ou l'expiration du blocage remet le compteur à zéro.

### #16 — Suivi du cycle de vie et du diagramme D4 (bonus)
- **Priorité : Could** · **Renvois :** D4 · EF4 · EF10 · EF11 · RG10 · H1/H3/H7
- **Critères :** le formateur distingue visuellement DÉPOSÉ, EN ATTENTE avec/sans relecteur, RELU et les états verrouillés ; le diagramme D4 versionné décrit les mêmes transitions que le modèle et le contrat.

---

### Récapitulatif de couverture

| Exigence | Issue(s) | Priorité |
|---|---|---|
| EF1 | #2 #10 | Must |
| EF2 | #1 #11 | Must |
| EF3 | #3 #11 | Must |
| EF4 | #4 #12 #16 | Must |
| EF5 | #5 #12 | Must |
| EF6 | #6 #10 | Must |
| EF7 | #4 | Must |
| EF8 | #8 #10 | Must |
| EF9 | #14 | Should |
| EF10 | #14 | Should |
| EF11 | #13 #10 #16 | Must |
| EF12 | #15 | Should |
| EF13 | #7 #11 | Must |
| EF14 | #9 | Must |
| EF15 | #13 #10 | Must |
| EF16 | #12 #14 | Must |
| **Trous** | H1→#13 · H2→#5 · H3→#4/#16 · H4→#7/#15 · H5→#15 · H6→#8 · H7→#13 · H8→#12/#14 · H9→#9 · H10→#4/#6 | — |

> **Priorisation assumée :** les exigences Must couvrent le besoin, les écrans F2, la fin et la
> clôture, l'affectation, l'anonymat et la cohérence du contrat. Les Should portent sur la
> correction/remplacement et l'anti-devinette ; le Could #16 est un suivi de bonus. Si le temps
> manque, sacrifice Could puis Should, mais ne déclare jamais Q10/Q12 complets sans #13.
