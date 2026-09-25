#!/usr/bin/env bash
# Cree les issues du backlog (etape 1) — source: docs/BACKLOG.md
set -e
cd "$(dirname "$0")"

mk() { # $1=title $2=label $3=body
  gh issue create --title "$1" --label "$2" --body "$3"
}

mk "EF2 — En tant qu'étudiant, je marque ma présence avec le code de la session" "Must" \
"### Critères d'acceptation
- \`POST /api/presences {code, etudiantId}\` valide → \`201 {id, sessionId, etudiantId, source:\"ETUDIANT\"}\`.
- Code inconnu → \`400 CODE_INCONNU\` ; expiré (>15 min) → \`410 CODE_EXPIRE\` ; déjà présent → \`409 DEJA_PRESENT\`.
- Toute erreur au format \`{code, message}\` via \`@RestControllerAdvice\` (B4), jamais de stack trace.

**Renvois :** EF2 · RG1 · RG2 · RG3 · D3
**Test :** intégration \`POST /api/presences\` (201 + 410 + 409)."

mk "EF1 — En tant que formateur, j'ouvre une session et j'obtiens un code qui expire" "Must" \
"### Critères
- \`POST /api/sessions {titre, promotionId}\` → \`201 {id, code, ouvertureAt, expirationAt}\`.
- \`expirationAt = ouvertureAt + 15 min\` (RG1).
- Champ manquant → \`400\` (format d'erreur imposé).

**Renvois :** EF1 · RG1"

mk "EF3 — En tant qu'étudiant, je dépose le lien de mon exercice pour une session" "Must" \
"### Critères
- \`POST /api/exercices {sessionId, etudiantId, lien}\` (URI) → \`201 {id, statut}\`.
- Lien invalide → \`400 LIEN_INVALIDE\` ; déjà déposé → \`409 EXERCICE_DEJA_DEPOSE\`.
- Dépôt possible jusqu'à la clôture de session (RG11/Q12).

**Renvois :** EF3 · RG11 · RG16 · D4"

mk "EF4 — En tant que système, j'assigne automatiquement un relecteur présent à chaque exercice" "Must" \
"### Critères
- Au dépôt : tirage **aléatoire** d'un étudiant **présent à la session**, **≠ auteur** (RG7, RG5).
- **1 seul** relecteur par exercice (\`relecture.exercice_id UNIQUE\`, RG6).
- Si aucun pair éligible → relecture créée **sans relecteur** (\`relecteur_id NULL\`) et visible au tableau (H3).

**Renvois :** EF4 · EF7 · RG5 · RG6 · RG7 · H3"

mk "EF5 — En tant que relecteur, je rends une note (0–20) et un commentaire" "Must" \
"### Critères
- \`POST /api/relectures/{id}\` (id = relecture assignée, H2) \`{note∈[0,20] entier, commentaire}\` → \`200\`.
- Note hors bornes / non entière → \`400 NOTE_INVALIDE\` (RG8).
- Auto-relecture → \`403 AUTO_RELECTURE\` (RG5) ; déjà rendue → \`409 RELECTURE_DEJA_RENDUE\`.

**Renvois :** EF5 · RG5 · RG8 · H2 · D2"

mk "EF6 — En tant que formateur, je vois le tableau par promotion" "Must" \
"### Critères
- \`GET /api/tableau?promotionId=\` → \`200\` la liste \`{etudiantId, nom, presences, exercicesDeposes, moyenne (nullable), relecturesEnAttente}\`.
- Promotion inconnue → \`404 PROMOTION_INCONNUE\`.
- **La moyenne est calculée par l'API, jamais recalculée côté frontend** (F3/ENF7).

**Renvois :** EF6 · RG10 · Q16 · F3"

mk "EF13 — En tant qu'étudiant, je choisis mon nom dans une liste (sans mot de passe)" "Must" \
"### Critères
- \`GET /api/etudiants?promotionId=\` alimente l'écran étudiant.
- Aucune saisie d'identifiant (Q1 — réponse non utile, retenue en exclusion d'auth H4).

**Renvois :** EF13 · H4 · Q1"

mk "EF8 — En tant que formateur, j'ajoute une présence à la main et elle se distingue" "Must" \
"### Critères
- \`POST /api/presences/manuelle {sessionId, etudiantId}\` → \`201 {…, source:\"FORMATEUR\"}\` (Q14/RG13).
- Déjà présent → \`409\` ; session clôturée → \`410 SESSION_CLOTUREE\`.
- Le tableau signale l'origine « ajouté par le formateur ».

**Renvois :** EF8 · RG13 · H6 · Q14"

mk "EF14 — L'identité du relecteur n'est jamais exposée à l'auteur relu" "Must" \
"### Critères
- Aucune réponse ne révèle le \`relecteurId\`/nom du relecteur à l'auteur.
- L'auteur ne voit que note + commentaire (Q8/RG14).

**Renvois :** EF14 · RG14 · Q8"

mk "F2 — Écran formateur (ouvrir une session + tableau)" "Must" \
"### Critères
- Écran Angular : formulaire d'ouverture + tableau live + ajout de présence + clôture.
- États **chargement/erreur** gérés ; appels via la **couche \`services/\`** dédiée (F3).

**Renvois :** F2 · EF1 · EF6 · EF8 · EF11"

mk "F2 — Écran étudiant (marquer sa présence + déposer son lien)" "Must" \
"### Critères
- Sélection de l'étudiant, saisie du code, dépôt du lien.
- Messages d'erreur contractuels affichés lisiblement ; via \`services/\` (F3).

**Renvois :** F2 · EF2 · EF3 · EF13"

mk "F2 — Écran relecteur (mes relectures à faire + rendu de note)" "Must" \
"### Critères
- Liste via \`GET /api/relectures/a-faire?etudiantId=\`.
- Formulaire note (0–20 entier) + commentaire, validation avant envoi.

**Renvois :** F2 · EF4 · EF5"

mk "EF11 — En tant que formateur, je clôture une session (lecture seule ensuite) [TROU H1]" "Should" \
"### Critères
- \`POST /api/sessions/{id}/cloture\` → \`200 {id, clotureAt}\`.
- Déjà clôturée → \`409 SESSION_DEJA_CLOTUREE\`.
- Après clôture : dépôt (RG11), correction (RG9), présence → refusés \`410 SESSION_CLOTUREE\` (RG15).

> Comble le **trou H1** : la clôture est supposée par Q10/Q11/Q12 mais jamais définie par le client.

**Renvois :** EF11 · RG9 · RG11 · RG15 · H1"

mk "EF9/EF10 — Correction de relecture et remplacement de lien avant échéance" "Should" \
"### Critères
- \`PUT /api/relectures/{id}\` autorisé tant que \`clotureAt IS NULL\` (sinon \`410\`) — arbitrage Q10.
- \`PUT /api/exercices/{id}\` refusé si relecture commencée (\`409 RELECTURE_COMMENCEE\`, RG12).

**Renvois :** EF9 · EF10 · RG9 · RG12 · arbitrage Q10"

mk "EF12 — Anti-devinette : blocage 2 min après 5 codes erronés" "Could" \
"### Critères
- 5 échecs (\`400 CODE_INCONNU\`) sur une session → le 6\`ᵉ\` renvoie \`429 TROP_ESSAIS\` pendant 120 s (RG4).
- Table \`code_tentative\` (D2).

**Renvois :** EF12 · RG4 · H5"

mk "D4 — Suivi des statuts d'exercice au tableau (bonus)" "Could" \
"### Critères
- Le formateur distingue DÉPOSÉ / EN ATTENTE / RELU (D4).
- Diagramme D4 versionné (fait à l'étape 1).

**Renvois :** D4 · RG10"

echo "===== Issues créées ====="
gh issue list --state open --limit 30
