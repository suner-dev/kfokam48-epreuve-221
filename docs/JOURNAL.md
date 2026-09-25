# Journal de bord — Ngansop Sumo Rainer · 221

> Une entrée **par étape**, écrite **au moment où je la termine**. Un journal rédigé d'un bloc à
> la fin se repère dans l'historique Git et ne compte pas → **je commets chaque entrée avec
> l'étape correspondante**.

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges complet (10 sections imposées, **16 exigences fonctionnelles EF1–EF16**,
**19 règles de gestion RG1–RG19**, 10 zones d'ombre/hypothèses H1–H10) ; les **4 diagrammes** en
Mermaid versionnés (D1 cas d'utilisation, D2 classes↔tables, D3 séquence présence↔codes HTTP,
**D4 états-transitions = bonus**) ; contrat `api/contrat.yaml` complété (5 opérations imposées
préservées, endpoints libres pour fin/clôture, détails, démarrage et résultats) ; **16 tickets de
backlog spécifiés** (13 Must / 2 Should / 1 Could), chacun relié à ses `EFx`/`RGx`. `.gitignore`
Java+JS posé avant tout code. Environnement vérifié.

**Bloqué :** ~15 min sur la **contradiction Q10 vs Q15** (relecture modifiable ou définitive) —
tranchée en faveur de **Q10** (règle détaillée et conditionnée vs intention générale de Q15), en
conservant le `POST` imposé et en ajoutant un **`PUT /api/relectures/{id}`** libre. Ensuite, le
contrôle a révélé deux trous structurels : la **fin de session absente malgré Q3/Q12** et la
distinction Q13 « commencé »/« rendu ». Ils sont désormais documentés par H7/H8, `finAt`,
`commenceeAt` et leurs endpoints. Enfin, l'audit GitHub a vérifié que le dépôt est public mais
qu'aucune issue n'est encore créée : le backlog reste 16 tickets spécifiés, pas 16 issues.

**IA :** m'a aidé à générer les squelettes Mermaid et à relire la conformité contrat↔annexe B.
**Vérification :** (1) chaque code HTTP des 5 opérations imposées a été comparé mot à mot au
contrat source ; (2) le YAML a été relu et validé par parseur après chaque ajout, sans doublon de
chemin ; (3) chaque titre d'issue a été relu sous l'angle « le client comprendrait-il ce résultat ? » ;
(4) D2 a été confronté aux tables et colonnes attendues et D3 aux codes HTTP, puis l'audit a recherché
les incohérences restantes. Rien n'a été accepté sans contrôle de mon côté.

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
**Bloqué :**
**IA :**
**Ce que je referais autrement avec une journée de plus :**
