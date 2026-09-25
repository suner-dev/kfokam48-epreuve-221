# Journal de bord — Ngansop Sumo Rainer · 221

> Une entrée **par étape**, écrite **au moment où je la termine**. Un journal rédigé d'un bloc à
> la fin se repère dans l'historique Git et ne compte pas → **je commets chaque entrée avec
> l'étape correspondante**.

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges complet (10 sections imposées, **14 exigences fonctionnelles EF1–EF14**,
**16 règles de gestion RG1–RG16**, 6 zones d'ombre/hypothèses H1–H6) ; les **4 diagrammes** en
Mermaid versionnés (D1 cas d'utilisation, D2 classes↔tables, D3 séquence présence↔codes HTTP,
**D4 états-transitions = bonus**) ; contrat `api/contrat.yaml` **complété et validé** (5 opérations
imposées à l'identique + 7 libres sous `/api`, 12 chemins, aucun doublon) ; **backlog de 16 issues**
(Must 12 / Should 2 / Could 2) chacune reliée à ses `EFx`/`RGx`. `.gitignore` Java+JS posé avant
tout code. Environnement vérifié.

**Bloqué :** ~15 min sur la **contradiction Q10 vs Q15** (relecture modifiable ou définitive) —
tranchée en faveur de **Q10** (règle précise et conditionnée vs intention générale de Q15), et le
piège a été de **respecter le `409 RELECTURE_DEJA_RENDUE` imposé** tout en honorant la correction :
résolu en gardant le `POST` pour le rendu initial et en ajoutant un **`PUT /api/relectures/{id}`**
libre pour la correction (≈10 min). ~10 min aussi sur le **trou H1** (« clôture de session » jamais
définie malgré Q10/Q11/Q12) : comblé par `clotureAt` + `POST /api/sessions/{id}/cloture` + RG15.
Enfin un aller-retour pour **fusionner les deux `/api/relectures/{id}`** (post+put sous une seule
clé YAML), corrigé et revérifié par parseur.

**IA :** m'a aidé à générer les squelettes Mermaid et à relire la conformité contrat↔annexe B.
**Vérification :** (1) chaque code HTTP des 5 opérations imposées a été comparé mot à mot à
`api/contrat.yaml` d'origine ; (2) le YAML est passé par un parseur (`yaml.safe_load`) → 12 paths,
**doublons = 0**, `relectures/{id}` = `[post, put]` ; (3) j'ai relu chaque titre d'issue sous l'angle
« le client comprendrait-il ce résultat ? » et écarté les libellés purement techniques ; (4) cohérence
D2 (classes) ↔ tables/contraintes et D3 ↔ statuts re-vérifiée manuellement. Rien n'a été accepté sans
contrôle de mon côté.

**À faire juste après :** créer les 16 issues sur GitHub, puis committer `[JALON] analyse` (AVANT
tout `spring init`).

---

## Étape 2 — Première version

**Fait :**
**Bloqué :**
**IA :**

---

## Étape 3 — Enveloppe

**Fait :**
**Bloqué :**
**IA :**
**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**
**Bloqué :**
**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**
**Bloqué :**
**IA :**

---

## Étape 6 — Soumission

**Fait :**
**Ce que je referais autrement avec une journée de plus :**
