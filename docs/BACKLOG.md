# Backlog — issues à créer sur `github.com/suner-dev/kfokam48-epreuve-221`

> Chaque issue : **titre = un résultat** (pas une tâche technique), **critères d'acceptation
> vérifiables**, **priorité Must/Should/Could**, **renvoi `EFx`/`RGx`**. À créer via
> `gh issue create` (ou l'UI) **pendant l'Étape 1**, avant `[JALON] analyse`.
> Règle d'or du sujet : **une branche par ticket, une PR par branche, issues fermées par les
> commits** (`Closes #n`). **Zéro issue = −10 pts** ; ce backlog en produit 14.

## Stories MUST (livrées en v0.1 — Étape 2)

### #1 — En tant qu'étudiant, je marque ma présence avec le code de la session
- **Renvois :** EF2 · RG1 · RG2 · RG3 · D3
- **Critères :**
  - `POST /api/presences {code, etudiantId}` valide → `201 {id, sessionId, etudiantId, source:"ETUDIANT"}`.
  - Code inconnu → `400 CODE_INCONNU` ; code expiré (>15 min) → `410 CODE_EXPIRE` ; déjà présent → `409 DEJA_PRESENT`.
  - Tout message d'erreur au format `{code, message}` (**B4**, jamais de stack trace).
- **Test :** intégration `POST /api/presences` (201 + 410 + 409).

### #2 — En tant que formateur, j'ouvre une session et j'obtiens un code qui expire
- **Renvois :** EF1 · RG1
- **Critères :** `POST /api/sessions {titre, promotionId}` → `201 {id, code, ouvertureAt, expirationAt}` avec `expirationAt = ouvertureAt + 15 min` ; champ manquant → `400`.

### #3 — En tant qu'étudiant, je dépose le lien de mon exercice pour une session
- **Renvois :** EF3 · RG11 · RG16 · D4
- **Critères :** `POST /api/exercices {sessionId, etudiantId, lien}` (URI) → `201 {id, statut}` ; lien invalide → `400 LIEN_INVALIDE` ; déjà déposé → `409 EXERCICE_DEJA_DEPOSE`.

### #4 — En tant que système, j'assigne automatiquement un relecteur présent à chaque exercice
- **Renvois :** EF4 · EF7 · RG5 · RG6 · RG7 · H3
- **Critères :** au dépôt, tirage **aléatoire** d'un étudiant **présent à la session**, **≠ auteur** ; **1 seul** relecteur par exercice (`relecture.exercice_id UNIQUE`) ; si aucun pair éligible → relecture créée **sans relecteur** (`relecteur_id NULL`) et visible au tableau.

### #5 — En tant que relecteur, je rends une note (0–20) et un commentaire
- **Renvois :** EF5 · RG5 · RG8 · H2 · D2
- **Critères :** `POST /api/relectures/{id}` (id = relecture assignée) `{note∈[0,20] entier, commentaire}` → `200` ; note hors bornes/non entière → `400 NOTE_INVALIDE` ; auto-relecture → `403 AUTO_RELECTURE` ; déjà rendue → `409 RELECTURE_DEJA_RENDUE`.

### #6 — En tant que formateur, je vois le tableau par promotion
- **Renvois :** EF6 · RG10 · Q16 · F3
- **Critères :** `GET /api/tableau?promotionId=` → `200` la liste `{etudiantId, nom, presences, exercicesDeposes, moyenne (nullable), relecturesEnAttente}` ; promotion inconnue → `404 PROMOTION_INCONNUE`. **La `moyenne` est calculée par l'API, jamais par le front** (ENF7/F3).

### #7 — En tant qu'étudiant, je choisis mon nom dans une liste (sans mot de passe)
- **Renvois :** EF13 · H4 · Q1 (réponse non utile, retenue en exclusion d'auth)
- **Critères :** `GET /api/etudiants?promotionId=` alimente l'écran étudiant ; aucune saisie d'identifiant.

### #8 — En tant que formateur, j'ajoute une présence à la main et elle se distingue
- **Renvois :** EF8 · RG13 · H6 · Q14
- **Critères :** `POST /api/presences/manuelle {sessionId, etudiantId}` → `201 {…, source:"FORMATEUR"}` ; déjà présent → `409` ; session clôturée → `410`. Le tableau signale l'origine formateur.

### #9 — En tant que relecteur, je ne suis jamais exposé à l'auteur que j'ai relu
- **Renvois :** EF14 · RG14 · Q8
- **Critères :** aucune réponse ne révèle `relecteurId`/nom à l'auteur ; l'auteur ne voit que note + commentaire.

### #10 — Écran formateur (ouvrir une session + tableau)
- **Renvois :** F2 · EF1 · EF6 · EF8 · EF11
- **Critères :** écran Angular avec formulaire d'ouverture + tableau live ; états **chargement/erreur** gérés ; appels via la **couche `services/`** dédiée (**F3**).

### #11 — Écran étudiant (présence + dépôt de lien)
- **Renvois :** F2 · EF2 · EF3 · EF13
- **Critères :** sélection de l'étudiant, saisie du code, dépôt du lien ; messages d'erreur contractuels affichés lisiblement.

### #12 — Écran relecteur (mes relectures à faire + rendu de note)
- **Renvois :** F2 · EF4 · EF5
- **Critères :** liste via `GET /api/relectures/a-faire?etudiantId=` ; formulaire note (0–20 entier) + commentaire ; validation avant envoi.

## Stories SHOULD (après l'enveloppe / v1.0 si le temps le permet)

### #13 — En tant que formateur, je clôture une session (elle devient lecture seule)
- **Renvois :** EF11 · RG9 · RG15 · **H1 (trou repéré)**
- **Critères :** `POST /api/sessions/{id}/cloture` → `200 {id, clotureAt}` ; déjà clôturée → `409 SESSION_DEJA_CLOTUREE` ; après clôture, dépôt (RG11)/correction (RG9)/présence refusés → `410 SESSION_CLOTUREE`.

### #14 — Correction de relecture et remplacement de lien avant échéance
- **Renvois :** EF9 · EF10 · RG9 · RG12 · arbitrage **Q10**
- **Critères :** `PUT /api/relectures/{id}` autorisé tant que `clotureAt IS NULL` (sinon `410`) ; `PUT /api/exercices/{id}` refusé si relecture commencée (`409 RELECTURE_COMMENCEE`).

## Stories COULD (si temps restant — backlog trié, non bloquant)

### #15 — Anti-devinette : blocage 2 min après 5 codes erronés
- **Renvois :** EF12 · RG4 · **H5** · table `code_tentative` (D2)
- **Critères :** 5 échecs (`400 CODE_INCONNU`) sur une session → le 6ᵉ renvoie `429 TROP_ESSAIS` pendant 120 s.

### #16 — Bonus D4 + tableau de suivi des statuts d'exercice
- **Renvois :** D4 · RG10
- **Critères :** le formateur distingue visuellement DÉPOSÉ / EN ATTENTE / RELU ; diagramme D4 versionné (déjà fait à l'étape 1).

---

### Récapitulatif de couverture

| Exigence | Issue(s) | Priorité |
|---|---|---|
| EF1 | #2 #10 | Must |
| EF2 | #1 #11 | Must |
| EF3 | #3 #11 | Must |
| EF4 | #4 #12 | Must |
| EF5 | #5 #12 | Must |
| EF6 | #6 #10 | Must |
| EF7 | #4 | Must |
| EF8 | #8 #10 | Must |
| EF9 | #14 | Should |
| EF10 | #14 | Should |
| EF11 | #13 #10 | Should |
| EF12 | #15 | Could |
| EF13 | #7 #11 | Must |
| EF14 | #9 #5 | Must |
| **Trous** | H1→#13 · H2→#5 · H3→#4 · H4→#7 · H5→#15 · H6→#8 | — |

> **Priorisation assumée :** les 12 Must couvrent intégralement le besoin exprimé (les 5 points)
> + les écrans F2 + la conformité contrat. Les Should/Could portent sur la **clôture** (trou) et
> le confort (correction, anti-cheat) — **sacrifiables en cas de retard sans casser la note
> produit**, conformément à la démarche (§10 du cahier).
