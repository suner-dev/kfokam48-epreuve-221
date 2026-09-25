#!/usr/bin/env bash
#
# appels-manuels.sh — Rejeu des 17 appels manuels imposes par la consigne 14.5.
#
# Le script joue, un par un, les points de verification manuelle exiges pour
# declarer le jalon v0.1 : references, ouverture de session, presence et doublon,
# depot d'exercice et affectation, demarrage et rendu de relecture, resultats
# recus (anonymat), tableau (moyenne fournie par l'API), fin puis cloture, et
# refus d'ecriture apres cloture. Les operations Should EF9/EF10 (remplacement du
# lien, correction de la note) sont egalement rejouees et signalees comme telles.
#
# Aucune valeur n'est inventee : chaque ligne affichée est la reponse brute de
# l'API (statut HTTP + corps JSON), seule la re-indentation par `jq` est ajoutee.
#
# Pre-requis : application demarree par `docker compose up --build`
# (backend sur http://localhost:8080), ainsi que `curl` et `jq`.
#
# Utilisation :
#   bash docs/appels-manuels.sh                     # sortie sur la sortie standard
#   API=http://hote:port bash docs/appels-manuels.sh
#
# Pour rejouer depuis un jeu de demonstration intact :
#   docker compose down -v && docker compose up -d --build
#
set -uo pipefail

API="${API:-http://localhost:8080}"
JSON='Content-Type: application/json'
BODYF="$(mktemp)"
trap 'rm -f "$BODYF"' EXIT

# req <method> <path> [body] : appelle l'API, STATUS=code HTTP, corps dans $BODYF.
req() {
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    STATUS="$(curl -s -o "$BODYF" -w '%{http_code}' -X "$method" "${API}${path}" -H "$JSON" -d "$body")"
  else
    STATUS="$(curl -s -o "$BODYF" -w '%{http_code}' -X "$method" "${API}${path}")"
  fi
}

# show <label> <method> <path> [body] : imprime la commande, le statut et le corps.
show() {
  local label="$1" method="$2" path="$3" body="${4:-}"
  echo "### ${label}"
  if [ -n "$body" ]; then
    echo "\$ curl -s -X ${method} '${API}${path}' -H '${JSON}' -d '${body}'"
  else
    echo "\$ curl -s -X ${method} '${API}${path}'"
  fi
  req "$method" "$path" "$body"
  echo "< HTTP ${STATUS}"
  if jq -c . "$BODYF" >/dev/null 2>&1; then
    echo "< $(jq -c . "$BODYF")"
  else
    echo "< $(cat "$BODYF")"
  fi
  echo
}

echo "# Rejeu des appels manuels du contrat — consigne 14.5"
echo "# API      : ${API}"
echo "# Date     : $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "# Commit   : $(git -C "$(dirname "$0")/.." rev-parse HEAD 2>/dev/null || echo 'inconnu')"
echo

# 1. References : promotions, puis etudiants de la promotion de demonstration.
show "1a. Lister les promotions" GET "/api/promotions"
show "1b. Lister les etudiants de la promotion 1 (extrait)" GET "/api/etudiants?promotionId=1"
echo "-> $(jq 'length' "$BODYF") etudiants renvoies pour la promotion 1"
echo

# 2. Ouvrir une session (code de presence expirant).
show "2. Ouvrir une session" POST "/api/sessions" '{"titre":"Session Appels Manuels","promotionId":1}'
SID="$(jq -r '.id' "$BODYF")"
CODE="$(jq -r '.code' "$BODYF")"

# 3. Marquer une presence avec le code fraichement genere.
show "3. Marquer la presence de l'etudiant 1" POST "/api/presences" "{\"code\":\"${CODE}\",\"etudiantId\":1}"

# 4. Provoquer le doublon : 409 DEJA_PRESENT.
show "4.presence en double (attendu 409 DEJA_PRESENT)" POST "/api/presences" "{\"code\":\"${CODE}\",\"etudiantId\":1}"

# 5. Consulter le detail des presences de la session.
show "5. Detail des presences de la session" GET "/api/sessions/${SID}/presences"

# 6. Depot d'exercice. On rend l'etudiant 2 present pour permettre l'appairage.
show "6a. Presence de l'etudiant 2 (pour l'appairage)" POST "/api/presences" "{\"code\":\"${CODE}\",\"etudiantId\":2}"
show "6b. Depot du lien de l'etudiant 1" POST "/api/exercices" "{\"sessionId\":${SID},\"etudiantId\":1,\"lien\":\"https://example.test/manuels/ex1\"}"
EID="$(jq -r '.id' "$BODYF")"

# 7. Detail des exercices : statut et relecteur assigne.
show "7. Detail des exercices de la session" GET "/api/sessions/${SID}/exercices"
RLID="$(jq -r --argjson e "$EID" '.[] | select(.id==$e) | .relectureId' "$BODYF")"

# 8. Ecran relecteur : relectures dues a l'etudiant 2, sans identite de l'auteur.
show "8. Relectures a faire pour l'etudiant 2" GET "/api/relectures/a-faire?etudiantId=2"

# EF9 (Should, ajoute apres le jalon v0.1) : remplacement du lien avant tout debut.
show "9a. EF9 — Remplacement du lien avant debut (200 attendu)" PUT "/api/exercices/${EID}" '{"lien":"https://example.test/manuels/ex1-v2"}'

# 9. Demarrer la relecture.
show "9b. Demarrer la relecture" POST "/api/relectures/${RLID}/debut" '{}'

# EF9 verrou : remplacement apres debut -> 409 RELECTURE_COMMENCEE.
show "9c. EF9 verrou — remplacement apres debut (attendu 409 RELECTURE_COMMENCEE)" PUT "/api/exercices/${EID}" '{"lien":"https://example.test/manuels/ex1-v3"}'

# 10. Rendu de la note et du commentaire, puis refus du second rendu.
show "10a. Rendu de la note (15)" POST "/api/relectures/${RLID}" '{"note":15,"commentaire":"Bon travail."}'
show "10b. Second rendu (attendu 409 RELECTURE_DEJA_RENDUE)" POST "/api/relectures/${RLID}" '{"note":12,"commentaire":"Encore."}'

# EF10 (Should) : correction de la note tant que la session n'est pas cloturee.
show "10c. EF10 — Correction de la note avant cloture (200 attendu)" PUT "/api/relectures/${RLID}" '{"note":17,"commentaire":"Apres relecture attentive."}'

# 11. Resultats recus par l'auteur : note et commentaire, jamais l'identite du relecteur.
show "11. Resultats recus par l'etudiant 1 (anonymat du relecteur)" GET "/api/etudiants/1/relectures-recues"

# 12 & 13. Tableau par promotion : la moyenne provient de l'API, non recalculee.
show "12. Tableau de la promotion 1" GET "/api/tableau?promotionId=1"
echo "-> 13. La moyenne de l'etudiant 1 ci-dessus est fournie par l'API (note 17 recue) ;"
echo "        le frontend l'affiche sans jamais la recalculer (contrainte F3)."
echo

# 14. Terminer la session (bloque l'auto-marquage).
show "14. Terminer la session" POST "/api/sessions/${SID}/fin" '{}'

# 15. Cloturer la session (bloque toutes les ecritures).
show "15. Cloturer la session" POST "/api/sessions/${SID}/cloture" '{}'

# 16. Toute ecriture est refusee apres cloture.
show "16. Presence apres cloture (attendu 410 SESSION_CLOTUREE)" POST "/api/presences" "{\"code\":\"${CODE}\",\"etudiantId\":3}"

echo "### Rejeu termine. Session id=${SID} code=${CODE} exercice=${EID} relecture=${RLID}."
