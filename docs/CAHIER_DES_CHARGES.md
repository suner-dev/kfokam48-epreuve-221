# Cahier des charges — KFOKAM48 : gestion de présence et relecture par les pairs

**Auteur :** Ngansop Sumo Rainer · 221
**Version :** 1.1 · **Date :** 25/09/2026
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

Trois événements de vie doivent rester distincts : l'**ouverture**, la **fin** du cours et la
**clôture administrative**. Le code et l'auto-marquage expirent ou cessent à la fin ; le dépôt,
la relecture et leur correction restent possibles après la fin, mais seulement jusqu'à la clôture.
Le formateur peut clôturer directement une session encore ouverte ; cette action termine aussi le
cours. Cette décision comble le trou H7 (§7).

L'objectif n'est pas la beauté de l'interface (le rendu visuel n'est pas noté) mais un outil
correct, conforme à un contrat d'API imposé, dont les règles de gestion sont respectées et
testables. Population visée : promotions de l'ordre de quelques dizaines d'étudiants, usage sur
téléphone pour l'étudiant, sur poste fixe pour le formateur.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session (→ code) ; la **terminer** puis la **clôturer** ; ajouter une présence à la main (Q14) ; consulter le tableau agrégé et le détail par session (Q16) | Marquer sa propre présence ; déposer un exercice ; relire ; modifier une relecture rendue par un étudiant |
| **Étudiant** | Choisir son nom dans une liste (Q1, pas de mot de passe) ; marquer sa présence avec le code pendant l'ouverture et avant expiration (Q2, Q3) ; déposer / remplacer le lien de son exercice jusqu'à la clôture (Q12, Q13) ; voir la note + commentaire reçus, **sans identité du relecteur** (Q8) | Marquer sa présence après la fin ou avec un code expiré ; déposer deux exercices pour la même session (unicité du contrat) ; relire son propre exercice (Q5) |
| **Relecteur** | Voir les relectures qui lui sont assignées ; **commencer** sa relecture ; rendre une note entière 0–20 + commentaire (Q9) ; **corriger** sa relecture tant que la session n'est pas clôturée (Q10) | Être assigné à son propre exercice (Q5) ; modifier le lien après le début de la relecture (Q13) ; corriger après clôture (Q10 borné) |

**Tranchage (§2, conséquence sur le modèle de données) :** le *relecteur* n'est **pas un acteur
distinct** — c'est un **étudiant** tenu d'exécuter une relecture qui lui a été **assignée**. En
conséquence, il n'existe pas d'entité « Relecteur » : une affectation `Relecture` relie un exercice,
un `Etudiant.relecteurId` obligatoire lorsqu'un pair est disponible, puis éventuellement une note.
Une affectation sans `relecteurId` existe seulement pour conserver le cas H3 en attente ; elle ne
donne pas le rôle de relecteur. Le rôle de relecteur est donc porté par une affectation **active et
attribuée** à cet étudiant.

## 3. Périmètre

**Inclus dans le périmètre fonctionnel visé (Must, puis Should/Could selon le temps) :**
- Ouverture, **fin**, clôture de session et code de présence à expiration (Q2, trous H1/H7).
- Marquage de présence par code, présence ajoutée par le formateur (Q14), unicité d'une présence.
- Dépôt du lien d'un exercice, remplacement tant que la relecture n'a pas commencé (Q12, Q13).
- Assignment **automatique** d'un relecteur (tirage au hasard parmi les présents, jamais soi-même)
  (Q6, Q7, Q5).
- Début, rendu d'une relecture (note entière 0–20 + commentaire) et **correction avant clôture** (Q9, Q10).
- Consultation par l'auteur de la note et du commentaire reçus, sans identité du relecteur (Q8).
- Tableau du formateur agrégé et détails par session : présence/source (Q14, Q16), exercices dont la
  relecture reste due (Q11), relectures dues par étudiant (Q16).
- 3 écrans frontend (formateur / étudiant / relecteur) (F2) ; données de démonstration au démarrage.

**Explicitement exclu (et pourquoi) :**
- **Authentification / mots de passe** : Q1 demande le choix d'un nom dans une liste, « ne perdez
  pas de temps là-dessus ». L'implémentation d'une authentification est donc hors projet ; la
  sélection explicite de l'identité dans une liste reste incluse (EF13) parce qu'elle est nécessaire
  pour alimenter `etudiantId` sans inventer une autre règle.
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
| **EF2** | L'étudiant marque sa présence avec le code | Quand j'envoie un `{code, etudiantId}` valide, avant expiration et avant la fin de session, alors `201 {id, sessionId, etudiantId, source=ETUDIANT}` et la présence apparaît dans le détail de la session | Must |
| **EF3** | L'étudiant dépose le lien de son exercice pour une session | Quand j'envoie `{sessionId, etudiantId, lien}` valide avant la clôture, alors `201`, une seule affectation est créée et le statut indique l'attente de relecture ; après la fin, le dépôt reste autorisé jusqu'à la clôture | Must |
| **EF4** | Le système affecte un pair à chaque exercice | Quand un exercice est déposé et qu'au moins un étudiant présent à cette session est différent de l'auteur, alors le système en choisit **au hasard** exactement un et crée l'affectation ; sinon l'exercice reste visible en attente sans relecteur | Must |
| **EF5** | Le relecteur rend une note et un commentaire | Quand j'envoie `{note∈[0,20] entier, commentaire}` sur ma relecture assignée non encore rendue et avant la clôture, alors `200` et la note est comptée dans la moyenne de l'auteur | Must |
| **EF6** | Le formateur consulte le tableau par promotion | Quand j'appelle `GET /api/tableau?promotionId=`, alors je reçois les compteurs imposés ; les endpoints de détail donnent en plus présence/source par session et exercices dont la relecture est due (Q11/Q14/Q16) | Must |
| **EF7** | Un exercice possède au plus une affectation de relecture | Quand le dépôt ou une réévaluation traite de nouveau le même exercice, alors aucune seconde ligne `Relecture` n'est créée (`exerciceId` unique) | Must |
| **EF8** | Le formateur ajoute une présence à la main | Quand le formateur marque la présence d'un étudiant avant clôture, alors `201` contient `source=FORMATEUR` et le détail de la session expose cette source | Must |
| **EF9** | L'étudiant remplace le lien de son exercice | Quand je modifie mon lien avant tout `commenceeAt` de la relecture, alors le lien est mis à jour ; dès le début de relecture, il est refusé | Should |
| **EF10** | Le relecteur corrige sa relecture avant clôture | Quand je modifie ma note/commentaire et que la session n'est pas clôturée, alors la relecture reste rendue, est mise à jour et l'exercice reste `RELU` | Should |
| **EF11** | Le formateur clôt une session | Quand j'appelle la clôture, alors `clotureAt` est fixé, `finAt` l'est aussi si elle était encore ouverte, et toute écriture liée est ensuite refusée | Must |
| **EF12** | Anti-devinette des codes de présence | À partir de la cinquième saisie invalide faite sous l'`etudiantId` déclaré, alors les nouvelles tentatives sont refusées avec `400 TROP_ESSAIS` pendant 120 secondes | Should |
| **EF13** | L'étudiant choisit son identité dans une liste | Quand j'ouvre l'écran étudiant, alors une liste d'étudiants de la promotion m'est proposée, sans mot de passe | Must |
| **EF14** | Confidentialité de l'identité du relecteur | Quand l'auteur consulte `GET /api/etudiants/{id}/relectures-recues`, alors il reçoit note et commentaire mais aucun nom ni identifiant de relecteur | Must |
| **EF15** | Le formateur termine une session | Quand j'appelle la fin d'une session ouverte, alors `finAt` est fixé, l'auto-marquage cesse, mais le dépôt et la relecture restent possibles jusqu'à la clôture | Must |
| **EF16** | Le relecteur commence une affectation attribuée | Quand j'appelle `POST /api/relectures/{id}/debut`, alors `commenceeAt` est fixé et le remplacement du lien devient impossible jusqu'à la clôture | Must |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| **ENF1** | Écran étudiant utilisable sur un téléphone (viewport, cibles tactiles) | Ouverture sur un profil mobile 360×640, aucune saisie impossible |
| **ENF2** | Le tableau répond en < 2 s pour une promotion de 60 étudiants | Mesure chronométrée de `GET /api/tableau` avec le jeu de démo (ou test d'intégration temporisé) |
| **ENF3** | Démarrage en ≤ 3 commandes (ou `docker compose up`) depuis un clone vierge | Exécution pas-à-pas du `README` dans un dossier vide |
| **ENF4** | Schéma de base **versionné** et reproductible | Migrations Flyway commitées ; `ddl-auto` ≠ `update` hors tests |
| **ENF5** | Données de démonstration réalistes chargées au démarrage | Au premier lancement : 60 étudiants, au moins deux sessions, présences ETUDIANT/FORMATEUR, un exercice RELU et un exercice en attente avec/sans relecteur |
| **ENF6** | Gestion d'erreurs **centralisée** et format d'erreur contractuel `∀` erreur | Aucun corps d'erreur par défaut Spring ; tests sur les cas 400/403/404/409/410 |
| **ENF7** | Aucune règle métier dupliquée frontend/backend (not. moyenne) | Le frontend n'affiche que ce que renvoie l'API (revue de code + F3) |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| **RG1** | Un code de présence expire **15 min** après l'ouverture ; à `now >= expirationAt`, il est refusé (`410 CODE_EXPIRE`) | Q2 |
| **RG2** | L'auto-marquage est permis seulement avant `finAt` ; une fin de session l'interdit sans clore les dépôts | Q3 + hypothèse H7 |
| **RG3** | Un étudiant ne peut marquer sa présence **qu'une fois** par session (unicité `(sessionId, etudiantId)`) → sinon `409 DEJA_PRESENT` | contrat imposé `409 DEJA_PRESENT` |
| **RG4** | À partir de la 5e saisie invalide faite sous un `etudiantId` déclaré, bloquer les nouveaux essais pendant **120 s**, puis remettre le compteur à zéro | Q4 + hypothèses H4/H5 |
| **RG5** | Un étudiant **ne peut jamais** relire son propre exercice → `403 AUTO_RELECTURE` | Q5 |
| **RG6** | **Un seul relecteur attribué au plus** par exercice | Q6 |
| **RG7** | Le relecteur est choisi **par le système, au hasard, parmi les étudiants présents à cette session**, différents de l'auteur | Q7 |
| **RG8** | La note est un **entier de 0 à 20** inclus → sinon `400 NOTE_INVALIDE` | Q9 |
| **RG9** | Une relecture peut être **corrigée tant que la session n'est pas clôturée** ; l'exercice reste `RELU` | Q10 (arbitrage §7 C1) |
| **RG10** | Un exercice dont la relecture n'est pas rendue reste « **en attente** » et est visible au formateur via le détail de session | Q11 |
| **RG11** | Un exercice peut être déposé après la fin mais **jusqu'à la clôture** de la session | Q12 + hypothèse H7 |
| **RG12** | Le lien est remplaçable tant que `Relecture.commenceeAt IS NULL` ; le simple fait que la note ne soit pas rendue ne suffit pas | Q13 + hypothèse H8 |
| **RG13** | Une présence ajoutée manuellement est marquée `source=FORMATEUR` et cette source est visible dans le détail de session | Q14 |
| **RG14** | L'auteur ne voit jamais le nom du relecteur ; son identifiant est aussi masqué pour empêcher la ré-identification | Q8 + hypothèse H9 |
| **RG15** | Une session **clôturée** est en lecture seule (plus de dépôt, auto-présence, remplacement, rendu ou correction) | Q10/Q12 + hypothèse H1 |
| **RG16** | Un même étudiant ne dépose **qu'un seul** exercice par session → sinon `409 EXERCICE_DEJA_DEPOSE` | contrat imposé `409 EXERCICE_DEJA_DEPOSE` |
| **RG17** | `finAt` et `clotureAt` sont deux événements distincts ; la clôture directe termine aussi la session si nécessaire | hypothèse H7 |
| **RG18** | Le premier `POST /api/relectures/{id}/debut` ou la première soumission de note fixe `commenceeAt` ; l'opération est idempotente dans son effet | Q13 + hypothèse H8 |
| **RG19** | `relecturesEnAttente` compte les affectations dues à l'étudiant ; les exercices sans pair relèvent d'un indicateur Q11 distinct | Q11/Q16 + hypothèse H10 |

## 7. Zones d'ombre, hypothèses et contradictions tranchées

### 7.1 Contradiction relevée (obligatoire — le sujet annonce 2 réponses en conflit)

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** (« un relecteur peut **corriger** sa note tant que le formateur n'a pas clôturé ») **vs Q15** (« la note est **définitive** une fois envoyée, il ne peut plus y revenir ») | **Je retiens Q10** (correction possible avant clôture) | Q10 est une règle **détaillée et conditionnée** ; Q10 et Q12 donnent explicitement une borne temporelle par la clôture. Q11 confirme qu'un exercice peut rester non relu, sans définir à lui seul la clôture. Q15 exprime une intention générale sans mécanisme ni échéance. **Conséquence API :** `POST /api/relectures/{id}` reste le rendu initial et renvoie `409` si déjà rendue ; `PUT /api/relectures/{id}` corrige jusqu'à la clôture. L'exercice demeure `RELU` après correction. |

### 7.2 Trous que personne n'a comblés (le sujet annonce ≥ 1 trou)

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| **H1 — La clôture de session est partout supposée, jamais définie.** Q10 et Q12 mentionnent la clôture, mais aucune question ne définit qui clôture, comment, ni ce qu'elle verrouille. | Hypothèse : le formateur clôt ; `clotureAt` est non modifiable et verrouille toute écriture liée. | `POST /api/sessions/{id}/cloture`, `Session.clotureAt`, **RG15**. | Q10/Q12 deviennent des règles exécutables plutôt que des intentions. |
| **H2 — `POST /api/relectures/{id}` : que désigne `{id}` ?** Le corps imposé ne porte aucune identité de relecteur. | `{id}` est l'identifiant de l'affectation `Relecture` créée au dépôt ; elle porte `exerciceId` et `relecteurId`. | Le backend dérive l'acteur de l'affectation, vérifie l'absence d'auto-relecture et le doublon. | Aucun champ supplémentaire n'est ajouté au corps imposé. |
| **H3 — Que faire si aucun pair éligible n'est présent ?** Q7 n'explique pas le cas où seul l'auteur est présent. | Créer une affectation unique avec `relecteurId = null` et le statut `EN_ATTENTE_SANS_RELECTEUR`. | Le détail des exercices de session rend Q11 visible ; cet état ne compte dans les relectures dues d'aucun étudiant. | Aucun relecteur fantôme et aucune présence forcée. |
| **H4 — Sans mot de passe (Q1), comment le backend sait-il « qui » agit ?** | L'identité déclarée est `etudiantId`, choisi dans la liste ; aucune authentification ni garantie anti-usurpation. | EF13 fournit la liste ; le risque est documenté et Q4 ne peut être contourné que de façon volontairement externe au client. | Q1 respectée sans ajouter d'authentification. |
| **H5 — Le blocage anti-devinette (Q4/RG4) : quel périmètre et quel statut ?** Un code inconnu n'a pas de `sessionId` fiable. | Compter les saisies invalides par `etudiantId` déclaré ; à partir de la 5e, répondre `400 TROP_ESSAIS` pendant 120 s. | Conserve exactement les statuts imposés ; pas de nouveau `429`. Le compteur est remis à zéro après le blocage ou un succès. | Plus cohérent avec B2 que le 429 initialement envisagé. |
| **H6 — Présenter un étudiant après l'expiration du code ?** | L'auto-marquage cesse ; le formateur peut encore ajouter `source=FORMATEUR` jusqu'à la clôture. | `POST /api/presences` → `410 CODE_EXPIRE` ; endpoint manuel disponible. | La correction manuelle Q14 est conservée. |
| **H7 — La fin de session n'existe pas dans le modèle, bien que Q3 la distingue de la clôture Q12.** | Ajouter `finAt` et `POST /api/sessions/{id}/fin` ; à la fin, blocage de l'auto-marquage seul. Une clôture directe fixe aussi `finAt` si nécessaire. | **EF15, RG2/RG11/RG17**, D2, D3, D4 et endpoints de session sont alignés. | Élimine la confusion qui rendait RG2 et RG11 inapplicables. |
| **H8 — Que signifie « commencé » pour Q13 ?** | Le premier appel explicite de démarrage ou la première soumission de note fixe `commenceeAt` ; le remplacement du lien est alors refusé, même avant la note. | `POST /api/relectures/{id}/debut`, **EF9/EF16, RG12/RG18**. | Respecte littéralement Q13 au lieu de le confondre avec « note non rendue ». |
| **H9 — Q8 masque-t-il seulement le nom ou aussi l'identifiant du relecteur ?** | Masquer les deux dans les réponses destinées à l'auteur ; ne pas exposer l'auteur au relecteur dans la liste des tâches. | **EF14/RG14** ; endpoint `/relectures-recues` sans `relecteurId`. | Confidentialité renforcée, hypothèse locale explicite. |
| **H10 — Une affectation sans relecteur doit-elle créer une ligne de relecture et qui la compte ?** | Créer une seule affectation par exercice avec `relecteurId` nullable ; `relecturesEnAttente` du tableau impose compte seulement les affectations attribuées à l'étudiant. | Distinguer l'indicateur Q11 (exercices en attente) de l'indicateur Q16 (tâches dues à l'étudiant). | Évite de faire croire qu'un exercice sans pair est dû à quelqu'un. |

**Note sur Q1 :** l'implémentation d'une authentification n'est pas utile dans le budget et reste
exclue ; la sélection d'un nom dans une liste est en revanche une exigence fonctionnelle minimale
(EF13) et l'identité déclarée sert uniquement de support technique, sans sécurité. La signaler montre
que le tri demandé par `CLIENT.md` ne conduit pas à ignorer une décision utile.

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
- **Stratégie de tests** : tests unitaires sur la moyenne, la note entière 0–20, l'expiration et
  les transitions ; tests d'intégration sur `POST /api/presences` (201/400/409/410), la fin de
  session, le démarrage de relecture et l'unicité de présence/exercice.
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
| 1.1 | 25/09/2026 | Audit de cohérence : fin de session distincte de la clôture (H7), démarrage de relecture distinct de la note rendue (H8), anonymat et compteurs Q11/Q16 précisés (H9/H10), priorité des dépendancesMust revue, endpoints de détail et backlog corrigés. |
| _2 (à venir)_ | _après Étape 3_ | _Mettre à jour suite à l'enveloppe (bug + changement de besoin) : sections impactées et diagrammes D2/D3. Le sujet rend une partie de l'analyse fausse → correction obligatoire et tracée ici._ |
