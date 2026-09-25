# Cahier des charges — KFOKAM48 : gestion de présence et relecture par les pairs

**Auteur :** Ngansop Sumo Rainer · 221
**Version :** 1 · **Date :** 25/09/2026
**Centre :** Yaoundé · **Compte GitHub :** suner-dev · **Dépôt :** `kfokam48-epreuve-221`
**Frontend choisi :** Angular, parce que son socle complet (injection de dépendances,
`HttpClient`, RxJS/`signal`) impose une couche de service dédiée qui centralise les appels API —
exactement ce qu'exige F3 (la moyenne vient de l'API, jamais recalculée côté client) — et rend
proprement les états de chargement et d'erreur.

> Ce document découle de l'analyse de `SUJET.md`, `CLIENT.md` (16 réponses) et `api/contrat.yaml`.
> Chaque décision métier cite son numéro de question (`Qx`). Toute hypothèse que le client n'a
> pas tranchée est écrite en **section 7** (jamais silencieuse).

---

## 1. Contexte et objectif

La direction de la formation KFOKAM48 organise des sessions de cours et doit, aujourd'hui encore,
suivre à la main trois choses qui se recoupent mal : qui était présent, qui a rendu son exercice,
et qui l'a corrigé. Elle veut une application web unique qui couvre le cycle complet d'une
session :

- le **formateur** ouvre une session et obtient un **code de présence** éphémère ;
- l'**étudiant** marque sa présence avec ce code, puis **dépose le lien** de son exercice ;
- le système **assigne un relecteur** (un pair présent) à chaque exercice ; le **relecteur** rend
  une note (0–20) et un commentaire ;
- le formateur consulte un **tableau** consolidé : présences, exercices déposés, moyenne reçue et
  relectures encore dues, par étudiant.

L'objectif n'est pas la beauté de l'interface (le rendu visuel n'est pas noté) mais un outil
correct, conforme à un contrat d'API imposé, dont les règles de gestion sont respectées et
testables. Population visée : promotions de l'ordre de quelques dizaines d'étudiants, usage sur
téléphone pour l'étudiant, sur poste fixe pour le formateur.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session (→ code) ; **clôturer** une session ; ajouter une présence à la main (Q14) ; consulter le tableau (Q16) | Marquer sa propre présence ; déposer un exercice ; relire ; modifier une relecture déjà rendue par un étudiant |
| **Étudiant** | Choisir son nom dans une liste (Q1, pas de mot de passe) ; marquer sa présence avec le code (Q2) ; déposer / remplacer le lien de son exercice (Q12, Q13) ; voir la note + commentaire reçus, **sans le nom du relecteur** (Q8) | Marquer sa présence hors ouverture de session (Q3) ou avec un code expiré (Q2) ; déposer deux exercices pour une même session (Q6/unicité) ; relire son propre exercice (Q5) |
| **Relecteur** | Voir la/les relecture(s) qui lui **son**t assignées ; rendre une note entière 0–20 + commentaire (Q9) ; **corriger** sa relecture tant que la session n'est pas clôturée (Q10) | Être assigné à son propre exercice (Q5) ; voir l'identité de l'auteur au-delà de ce qu'exige la correction ; corriger après clôture (Q10 borné) |

**Tranchage (§2, conséquence sur le modèle de données) :** le *relecteur* n'est **pas un acteur
distinct** — c'est un **étudiant** tenu d'exécuter une relecture qui lui a été **assignée**. En
conséquence, il n'existe pas d'entité « Relecteur » : une relecture est une association
`Exercice` ↔ `Étudiant(relecteurId)`, produite par le tirage aléatoire (Q7). Le « rôle » de
relecteur est donc un **état** d'un étudiant, porté par l'existence d'une ligne `Relecture`.

## 3. Périmètre

**Inclus dans cette version (Must + Should + Could du backlog) :**
- Ouverture / clôture de session et code de présence à expiration (Q2, trou « clôture » §7 H1).
- Marquage de présence par code, présence ajoutée par le formateur (Q14), unicité d'une présence.
- Dépôt du lien d'un exercice, remplacement tant que la relecture n'a pas commencé (Q12, Q13).
- Assignment **automatique** d'un relecteur (tirage au hasard parmi les présents, jamais soi-même)
  (Q6, Q7, Q5).
- Rendu d'une relecture (note entière 0–20 + commentaire) et **correction avant clôture** (Q9, Q10).
- Tableau du formateur : présences, exercices déposés, moyenne reçue, relectures dues (Q16).
- 3 écrans frontend (formateur / étudiant / relecteur) (F2) ; données de démonstration au démarrage.

**Explicitement exclu (et pourquoi) :**
- **Authentification / mots de passe** : Q1 demande le choix d'un nom dans une liste, « ne perdez
  pas de temps là-dessus ». → **réponse non utile** au vrai besoin, retenue comme périmètre hors
  projet (voir §7).
- **Gestion des comptes/promotions hors normes** (CRUD promotions, inscriptions) : promotions et
  étudiants sont des **données de référence** chargées en démo, pas un module à développer.
- **Téléversement de fichiers** : seul un **lien URI** est déposé (`lien`, `format: uri` du contrat).
- **Historique de versions d'un lien** : Q13 autorise le **remplacement**, pas le versioning.
- **Notifications / e-mails**, **export PDF/Excel du tableau**, **mode hors-ligne** : hors budget
  temps d'une journée, non demandés.
- **Anti-cheat avancé au-delà du blocage 2 min** (Q4) : hors scope, seul le blocage simple est fait.

## 4. Exigences fonctionnelles

Priorités **Must / Should / Could**. Un critère se lit « quand … alors … ».

| Réf | Exigence | Critère d'acceptation vérifiable | Priorité |
|---|---|---|---|
| **EF1** | Le formateur ouvre une session et obtient un code de présence | Quand j'envoie `{titre, promotionId}`, alors je reçois `201 {id, code, ouvertureAt, expirationAt}` avec `expirationAt = ouvertureAt + 15 min` | Must |
| **EF2** | L'étudiant marque sa présence avec le code | Quand j'envoie un `{code, etudiantId}` valide et non expiré, alors `201 {id, sessionId, etudiantId, source=ETUDIANT}` et l'étudiant apparaît « présent » au tableau | Must |
| **EF3** | L'étudiant dépose le lien de son exercice pour une session | Quand j'envoie `{sessionId, etudiantId, lien}` (URI valide) et que j'ai un exercice non déposé pour cette session, alors `201 {id, statut=EN_ATTENTE_DE_RELECTURE}` | Must |
| **EF4** | Le système assigne un relecteur à chaque exercice | Quand un exercice est déposé, alors le système choisit **au hasard** un étudiant **présent à cette session**, **≠ auteur**, et crée une relecture liée | Must |
| **EF5** | Le relecteur rend une note et un commentaire | Quand j'envoie `{note∈[0,20] entier, commentaire}` sur ma relecture assignée non encore rendue, alors `200` et la note est comptée dans la moyenne de l'auteur | Must |
| **EF6** | Le formateur consulte le tableau par promotion | Quand j'appelle `GET /api/tableau?promotionId=`, alors je reçois, par étudiant : présences, exercices déposés, moyenne reçue, relectures en attente | Must |
| **EF7** | Un exercice peut avoir **un seul** relecteur | Quand un second relecteur est demandé pour le même exercice, alors il n'en est pas assigné (contrainte d'unicité) | Must |
| **EF8** | Le formateur ajoute une présence à la main | Quand le formateur marque la présence d'un étudiant (sans code), alors la présence est créée avec `source=FORMATEUR` et le tableau le signale | Must |
| **EF9** | L'étudiant remplace le lien de son exercice | Quand je modifie mon lien **avant** que la relecture ne soit rendue, alors le lien est mis à jour ; sinon refusé | Should |
| **EF10** | Le relecteur corrige sa relecture avant clôture | Quand je modifie ma note/mon commentaire **et que la session n'est pas clôturée**, alors la relecture est mise à jour | Should |
| **EF11** | Le formateur clôture une session | Quand j'appelle la clôture d'une session ouverte, alors elle est marquée clôturée (`clotureAt`) et toute écriture dessus est ensuite refusée | Should |
| **EF12** | Anti-devinette des codes de présence | Quand un étudiant échoue **5 fois** sur le même code, alors il est bloqué **2 minutes** pour cet essai | Should |
| **EF13** | L'étudiant choisit son identité dans une liste | Quand j'ouvre l'écran étudiant, alors une liste d'étudiants (par promotion) m'est proposée, sans mot de passe | Must |
| **EF14** | Confidentialité de l'identité du relecteur | Quand l'auteur consulte sa note, alors ni le nom ni l'`id` du relecteur ne lui sont exposés (seulement note + commentaire) | Must |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| **ENF1** | Écran étudiant utilisable sur un téléphone (viewport, cibles tactiles) | Ouverture sur un profil mobile 360×640, aucune saisie impossible |
| **ENF2** | Le tableau répond en < 2 s pour une promotion de 60 étudiants | Mesure chronométrée de `GET /api/tableau` avec le jeu de démo (ou test d'intégration temporisé) |
| **ENF3** | Démarrage en ≤ 3 commandes (ou `docker compose up`) depuis un clone vierge | Exécution pas-à-pas du `README` dans un dossier vide |
| **ENF4** | Schéma de base **versionné** et reproductible | Migrations Flyway commitées ; `ddl-auto` ≠ `update` hors tests |
| **ENF5** | Données de démonstration chargées au démarrage (promotions, étudiants, 1 session) | Au premier lancement, le tableau n'est pas vide |
| **ENF6** | Gestion d'erreurs **centralisée** et format d'erreur contractuel `∀` erreur | Aucun corps d'erreur par défaut Spring ; tests sur les cas 400/403/404/409/410 |
| **ENF7** | Aucune règle métier dupliquée frontend/backend (not. moyenne) | Le frontend n'affiche que ce que renvoie l'API (revue de code + F3) |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| **RG1** | Un code de présence expire **15 min** après l'ouverture de la session ; après, il est refusé (`410 CODE_EXPIRE`) | Q2 |
| **RG2** | Une présence ne peut être marquée que **pendant** la session ouverte ; jamais après la fin | Q3 |
| **RG3** | Un étudiant ne peut marquer sa présence **qu'une fois** par session (unicité `(sessionId, etudiantId)`) → sinon `409 DEJA_PRESENT` | Q3 + tableau Q16 |
| **RG4** | Après **5 tentatives** de code échouées pour un étudiant, blocage de **2 minutes** | Q4 |
| **RG5** | Un étudiant **ne peut jamais** relire son propre exercice → `403 AUTO_RELECTURE` | Q5 |
| **RG6** | **Un seul relecteur** par exercice | Q6 |
| **RG7** | Le relecteur est choisi **par le système, au hasard, parmi les étudiants présents à cette session** | Q7 |
| **RG8** | La note est un **entier de 0 à 20** inclus → sinon `400 NOTE_INVALIDE` | Q9 |
| **RG9** | Une relecture peut être **corrigée tant que la session n'est pas clôturée** | Q10 (arbitrage §7 C1) |
| **RG10** | Un exercice dont la relecture n'est pas rendue reste « **en attente** » et est visible au formateur | Q11 |
| **RG11** | Un exercice peut être déposé **jusqu'à la clôture** de la session (pas seulement pendant le cours) | Q12 |
| **RG12** | Le lien d'un exercice est remplaçable **tant que sa relecture n'a pas commencé** (note non rendue) | Q13 |
| **RG13** | Une présence ajoutée manuellement est marquée `source=FORMATEUR` (et le tableau le montre) | Q14 |
| **RG14** | L'auteur d'un exercice **ne voit jamais** l'identité de son relecteur | Q8 |
| **RG15** | Une session **clôturée** est en lecture seule (plus de dépôt, de présence, de correction) | déduit de Q10/Q12, formalisé §7 H1 |
| **RG16** | Un même étudiant ne dépose **qu'un seul** exercice par session → sinon `409 EXERCICE_DEJA_DEPOSE` | Q16 (« nb exercices déposés ») + contrat |

## 7. Zones d'ombre, hypothèses et contradictions tranchées

### 7.1 Contradiction relevée (obligatoire — le sujet annonce 2 réponses en conflit)

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** (« un relecteur peut **corriger** sa note tant que le formateur n'a pas clôturé ») **vs Q15** (« la note est **définitive** une fois envoyée, il ne peut plus y revenir ») | **Je retiens Q10** (correction possible avant clôture) | Q10 est **opérationnelle et conditionnée** (« tant que la session n'est pas clôturée ») et **s'articule avec Q11** (le formateur voit ce qui reste en attente, donc il existe une phase vivante avant la clôture) ; Q15 n'est qu'une **intention générale** (« plus honnête ») sans condition. En cas de conflit entre une règle précise datée d'une intention floue, la règle précise l'emporte. **Conséquence API :** `POST /api/relectures/{id}` (rendu initial, `409` si déjà rendue) reste conforme à l'annexe ; la **correction** passe par une opération dédiée `PUT /api/relectures/{id}`, refusée après clôture (**RG9/RG15**). On honore ainsi **à la fois** le contrat figé et le choix Q10. |

### 7.2 Trous que personne n'a comblés (le sujet annonce ≥ 1 trou)

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| **H1 — La clôture de session est partout supposée, jamais définie.** Q10/Q11/Q12 disent « tant que le formateur n'a pas clôturé » / « jusqu'à ce que je clôture », mais **aucune question ne définit qui clôture, comment, ni ce que la clôture verrouille** — et aucune opération du contrat imposé ne l'expose. | Hypothèse : **seul le formateur clôture** ; la clôture fixe `clotureAt` (non modifiable) et **verrouille en écriture** la session. | J'ajoute `POST /api/sessions/{id}/cloture` (partie libre du contrat) et l'attribut `clotureAt` à `Session`. **RG15**. | Sans cela, Q10 et Q12 sont inapplicables : la « clôture » devient un concept de modélisation réel, pas implicite. |
| **H2 — `POST /api/relectures/{id}` : que désigne `{id}` ?** Le corps imposé ne porte **aucune identité** de relecteur, or il faut connaître le relecteur pour refuser l'auto-relecture (Q5) et rattacher la note. | Hypothèse : `{id}` = **identifiant de la `Relecture` (assignation)** créée par le système au tirage (RG7), qui porte déjà `relecteurId` et `exerciceId`. | Le backend dérive le relecteur de l'assignation, vérifie `auteur ≠ relecteur` (**403**), et refuse le doublon (**409** si note déjà présente). | Évite d'ajouter un champ hors contrat dans le corps imposé ; conforme B2. |
| **H3 — Que faire si aucun relecteur éligible n'est présent ?** RG7 tire parmi les présents ≠ auteur ; si la session ne compte qu'un seul présent (l'auteur) ou 0 pair valide. | Hypothèse : l'exercice reste **`EN_ATTENTE_DE_RELECTURE` sans relecteur assigné** (`relecteurId` null) et apparaît au formateur via `relecturesEnAttente` (Q11). | Pas d'assignation forcée ; le tableau montre l'anomalie. Aucune erreur client. | Le cas est **visible** (exigence Q11) sans inventer un relecteur fantôme. |
| **H4 — Sans mot de passe (Q1), comment le backend sait-il « qui » agit ?** | Décision : l'identité est **portée par la donnée d'entrée** (`etudiantId` fourni par le frontend après choix dans la liste). **Pas d'authentification** (hors scope §3) ; l'absence d'usurpation est une **limite assumée** du périmètre. | Conformément à Q1 (« ne perdez pas de temps là-dessus »). | Simplifie ; documente la limite. |
| **H5 — Le blocage anti-devinette (Q4/RG4) : quel code HTTP ?** Le contrat n'impose que 400/409/410 sur `POST /api/presences`. | Hypothèse : renvoyer **`429 TROP_ESSAIS`** (hors des trois imposés) au-delà de 5 échecs, dans le **format d'erreur standard** `{code, message}`. | Priorité **Should** (EF12). Alternative notée : défaut 400 si on veut rester strictement dans les codes listés. | Le B2 exige la **présence** des codes imposés, pas l'**exclusivité** ; la condition d'erreur est nouvelle mais cohérente. |
| **H6 — « présence à chaque session » vs code expiré (Q2/Q3/Q14) :** un étudiant peut-il être marqué présent **après expiration du code** ? | Décision : le code n'autorise l'**auto-marquage** que pendant les 15 min (RG1) ; **après expiration et avant clôture**, seule une présence `source=FORMATEUR` (Q14) est possible. | `POST /api/presences` (étudiant) → 410 après expiration ; `POST /api/presences/manuelle` (formateur) → autorisée tant que non clôturé. | Distingue clairement les deux voies (contrat `source` existe **pour ça**, cf. annexe B). |

**Note sur la « réponse non utile » :** **Q1** (mots de passe / connexion) ne structure aucune
règle du produit ; elle est retenue **uniquement** comme exclusion de périmètre (H4). La signaler
montre le tri demandé par `CLIENT.md`.

## 8. Contraintes techniques

**Reprises du sujet (non négociables) :**
- **B1** Java 17+ (poste : JDK 26), **Maven**, wrapper **`mvnw` commité**.
- **B2** Contrat `api/contrat.yaml` respecté **à la lettre** (chemins, verbes, statuts, format d'erreur).
- **B3** Séparation **contrôleur / service / repository** ; **DTO** obligatoires (aucune entité JPA exposée).
- **B4** **Validation des entrées** + erreurs centralisées via **`@RestControllerAdvice`**.
- **B5** Schéma **versionné** (Flyway) ; **`ddl-auto=update` interdit hors tests**.
- **B6** **1 test unitaire** (règle métier réelle) + **1 test d'intégration** (endpoint), sur poste vierge.
- **F1** Framework déclaré + justifié (Angular, en-tête) ; **build vert**.
- **F2** **3 écrans** : formateur, étudiant, relecteur.
- **F3** Couche API dédiée ; états loading/erreur ; **aucune logique métier dupliquée** (moyenne depuis l'API).

**Contraintes que je m'impose :**
- **Base de données** : **PostgreSQL** en exécution (via `docker compose`), **H2 en mémoire** pour
  les tests d'intégration (B6, poste vierge). Alternative retenue si le correcteur n'a pas Docker :
  profil `dev` sur **H2 fichier** + seed — documentée dans le `README`.
- **Migrations** : Flyway, nommage `V1__init.sql`, `V2__seed_demo.sql` ; **la 1ʳᵉ migration doit
  exister avant l'ouverture de l'enveloppe** (le sujet prévient qu'elle touche le schéma).
- **Stratégie de tests** : unitaire sur la **moyenne** et/ou le **contrôle note entière 0–20** +
  sur **expiration du code (RG1)** ; intégration sur `POST /api/presences` (201 + 410 + 409).
- **Format d'erreur** : DTO `{"code","message"}` appliqué **à toutes** les erreurs, y compris 404
  validation. Message **en français**, code **stable en MAJUSCULES**.

## 9. Livrables

- **Analyse (Étape 1)** : ce `CAHIER_DES_CHARGES.md`, les diagrammes `D1`–`D4` (`docs/diagrammes/`),
  le **backlog en issues** GitHub, `api/contrat.yaml` complété et **figé** → commit `[JALON] analyse`.
- **v0.1 (Étape 2)** : backend + frontend couvrant les stories **Must**, branches/PR/issues →
  commit `[JALON] v0.1`.
- **Étape 3** : conduite du changement (issue, reproduction du bug, migration, contrat à jour,
  cahier + diagrammes corrigés dans un commit qui le dit, re-priorisation écrite).
- **v1.0 (Étape 4)** : `[JALON] v1.0`, `CHANGELOG.md`, `README` testé depuis clone vierge, backlog trié.
- **Étape 5** : dépôt séparé `kfokam48-gitlab-221` (cinq situations Git).
- **Étape 6** : `SOUMISSION.md` téléversé (2 dépôts publics + 2 hash 40 car.).
- **Continus** : `docs/JOURNAL.md` (une entrée par étape, écrite à chaud).

## 10. Démarche prévue (comment je mène les six étapes)

1. **Analyse (le plus lourd, 38 pts)** — ce document, 4 diagrammes Mermaid, issues, contrat figé.
   **Aucun `spring init`** avant le jalon `[JALON] analyse`.
2. **v0.1** — Backend d'abord (contrat = spec) : entités + Flyway + DTO + contrôleurs conformes,
   puis les 3 écrans Angular branchés sur une couche `services/`. Une branche par ticket, PR, issues
   fermées par les commits. Jalon `[JALON] v0.1` **poussé** (débloque l'enveloppe).
3. **Enveloppe** — j'ouvre une issue **avant** de coder, je **reproduis** le bug, j'applique la
   **migration** qui en découle, je **mets à jour** contrat + cahier + diagrammes (commit dédié),
   je **re-priorise** le backlog par écrit, et je **sépare** correctif vs évolution.
4. **v1.0** — Stories Should/Could au mieux, `CHANGELOG.md`, `README` vérifié en clone vierge,
   données de démo, `[JALON] v1.0`.
5. **Épreuve Git** — dépôt séparé, `push --all`, force admis là seulement.
6. **Soumission** — relevé des hash **après** le dernier push, téléversement bien avant 18h00.

**Si je prends du retard :** je sacrifie l'ordre **Could → Should**, jamais l'hygiène Git ni la
cohérence contrat/B5 (schéma versionné). Un produit aux ¾ livré avec une démarche propre > un
produit parfait sans historique.

**Definition of Done — un ticket est terminé quand :**
- le code respecte **B3/B4** (DTO, validation, erreur centralisée) et la **règle `RGx`** citée ;
- le contrat `api/contrat.yaml` est **à jour** si l'endpoint change ;
- au moins **un test** (unitaire ou d'intégration) prouve le critère d'acceptation ;
- la **branch** associée est **reliée** à l'issue et fusionnée via **PR** sur `main` ;
- un **commit atomique** au message explicite (référence `EFx`/`RGx`) existe et est **poussé**.

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 25/09/2026 | Version initiale — analyse d'avant-code (14 EF, 16 RG, 1 contradiction Q10/Q15 tranchée, 6 zones d'ombre/hypothèses dont le trou « clôture »). |
| _2 (à venir)_ | _après Étape 3_ | _Mettre à jour suite à l'enveloppe (bug + changement de besoin) : sections impactées et diagrammes D2/D3. Le sujet rend une partie de l'analyse fausse → correction obligatoire et tracée ici._ |
