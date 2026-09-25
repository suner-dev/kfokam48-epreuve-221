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

**Fait :** Backend Spring Boot/PostgreSQL/H2, frontend Angular et parcours formateur/étudiant/relecteur intégrés. Les tickets Must sont livrés par branches, commits atomiques et PR ; le contrat, les tests backend et les tests Angular sont présents.
**Bloqué :** L’enveloppe reste fermée jusqu’à la validation complète de v0.1 ; aucun changement d’enveloppe n’a été appliqué.
**IA :** L’IA a aidé à produire les DTO API, les services Angular et les scénarios de test. J’ai comparé chaque endpoint livré à `api/contrat.yaml`, exécuté `./backend/mvnw -B verify`, `npm run build` et `npm test -- --watch=false`, puis corrigé les écarts Must de documentation et de reproductibilité.

---

## Étape 3 — Enveloppe

**Fait :** l'enveloppe est arrivée avec **deux demandes distinctes**, et je les ai traitées comme deux
sujets séparés, en deux branches et deux pull requests, comme l'exigence le demande.

**1. Le bug** (« deux étudiants côte à côte, un seul apparaît dans ma liste ») — **livré et vérifié**
(issue #67, PR #68). Traduit, il s'agissait d'une **perte de donnée** : l'étudiant tapait un code
valide, recevait `201`, et sa ligne de présence n'était jamais écrite. La cause n'était pas une
limite de débit mais une **consommation doublée du pool de connexions** : `REQUIRES_NEW` suspend la
transaction courante sans libérer sa connexion, donc chaque marquage en retenait deux pour un pool
de 5. Reproduit devant le correcteur : 6 marquages simultanés donnaient `500 500 500 201 201 201`.
Corrigé en sortant le comptage anti-devinette de la transaction de présence.

**2. Le changement de besoin** (« deux pairs par exercice, moyenne des deux, provisoire si un seul a
rendu ») — **réalisé et livré** (issues #70 et #71, PR #76).

**Ce qui a été livré** (PR #76, branche `feature/70-deux-relecteurs`) : l'analyse remise à jour
dans un commit qui le dit — RG6 barrée et marquée annulée, RG20 créée, EF17 créée, H13 et H14
ajoutées, contradiction secondaire Q8/Q9 tranchée par écrit, D2 en cardinalité `0..2` et D4 corrigé
sur le caractère provisoire ; la migration V4 et le contrat ; le backend complet, prouvé par
`ReviewSummaryTest` (5 tests) et `DeuxRelecteursIT` (3 tests de bout en bout). Vérifié :
`./backend/mvnw clean verify` → 35 tests unitaires et 37 tests d'intégration, 0 échec.

**Le blocage, et comment il a été levé :** V1 déclare `CONSTRAINT uk_relecture_exercice UNIQUE
(exercice_id)`. H2 2.3 — la base de test — conserve l'index unique créé implicitement pour cette
contrainte même après `ALTER TABLE ... DROP CONSTRAINT`, puis refuse `DROP INDEX` au motif que cet
index « appartient à une contrainte » (erreur 90085), et son nom réel est introuvable (erreur
90057). PostgreSQL, base d'exécution, n'a pas ce défaut. Quatre approches ont été essayées et
vérifiées par un build complet chacune : `DROP CONSTRAINT` simple, `DROP INDEX` nommé, une
migration Java introspective, puis la recréation de table. C'est la quatrième qui donne le même
schéma sur les deux bases, ce qu'exige la consigne 2.2.

**Le second obstacle, plus instructif :** la recréation fonctionnait, mais trois scénarios
d'atterrissage échouaient en `409 CONFLIT_CONCURRENCE` sans qu'aucun message ne désigne la
migration. La table recopiait la colonne `id`, l'identité repartait de 1 alors que les identifiants
1 à 4 étaient déjà pris par le seed, et la première insertion suivante butait sur la clé
primaire. Les identifiants ne sont désormais plus recopiés — `relecture` est une table feuille,
aucune table n'y fait référence, donc renuméroter ne casse aucune jointure. Le commentaire de la
migration l'explique, pour que personne ne « corrige » ce choix sans savoir pourquoi il a été fait.

**Pourquoi je n'ai pas fusionné avant :** la consigne 15 est explicite — « si une seule case est
fausse, ne pas poser le jalon ». À un moment, la fonctionnalité était prête et invérifiable ; j'ai
préféré la laisser non livrée et le dire trois fois plutôt que déclarer vérifiée une garantie
d'unicité à moitié supprimée.

**Bloqué :** ~3 h 20 au total sur l'étape 3, dont environ 45 min sur le seul schéma : quatre approches
distinctes pour la contrainte (DROP CONSTRAINT simple, DROP INDEX nommé, migration Java
introspective, recréation de table), chacune vérifiée par un build complet, puis le problème d'identité
de la table recréée.

**Ce que j'ai sorti du périmètre pour absorber le changement de besoin, et pourquoi :** les issues
**#58, #59 et #60** — la coquille applicative : en-tête « verre », bibliothèque de composants,
refonte de l'écran formateur (filtre, compteurs, rafraîchissement, raccourcis). Motifs écrits dans
la section 3 du cahier : le rendu visuel n'est pas noté et le CSS vaut zéro point ; ces tickets ne
changent aucun comportement métier, alors qu'un tableau au design soigné mais dont la
moyenne ne viendrait pas de l'API serait un piège ; ils sont de l'ordre de l'étape 4 alors que
l'enveloppe est notée à l'étape 3 ; et ils n'ont ni branche, ni commit, ni PR — les abandonner ne
supprime donc aucun travail déjà poussé. Je refuse en revanche de sacrifier EF9/EF10, EF12 et D4 :
ils sont mergés et vérifiés, les abandonner laisserait des `PUT` annoncés mais non livrés.

**IA :** l'IA a écrit le premier jet du correctif et de la migration. **Vérification :** je n'ai
accepté aucune de ses intuitions sans les éprouver — j'ai reproduit le bug moi-même sur le
conteneur, récupéré la cause dans le log du pool de connexions, écrit le test rouge avant le
correctif, et vérifié le correctif par `./backend/mvnw clean verify` (30 tests unitaires, 34 tests
d'intégration) puis par rejeu des 17 appels manuels. Pour le changement de besoin, j'ai vérifié
quatre migrations successives par un build complet et par la lecture du log Flyway : c'est
précisément parce que ces builds échouaient que j'ai refusé de fusionner.

---

## Étape 4 — Version finale

**Fait :** `CHANGELOG.md` cohérent avec l'historique, README remis à jour et rejoué depuis un
dossier vierge, backlog trié et issues fermées avec `Closes #n`.

**Bloqué :** l'étape 5 (épreuve Git) a été annulée et ne figure donc ni au journal ni au
`SOUMISSION.md`. Le second dépôt `kfokam48-gitlab-221` n'a pas été créé, et le sujet en prévoyait
deux : c'est une écart assumé et annoncé, pas un oubli.

**IA :** l'IA a produit le `CHANGELOG.md` et la relecture du README. **Vérification :** chaque
version du `CHANGELOG.md` a été confrontée aux tags et aux titres de commits réels, et non à un
récit ; le README a été rejoué depuis un dossier vide et non relu seulement.

---

## Étape 5 — Épreuve Git

**Annulée.** Ne compte pas dans le barème et ne figure pas dans le dépôt.

---

## Étape 6 — Soumission

**Fait :** `SOUMISSION.md` rédigé avec les adresses des dépôts et les hash de 40 caractères relevés
après le dernier push.

**Bloqué :** aucun. La soumission se fait sur la plateforme, hors de mon périmètre.

**IA :** l'IA a rédigé le fichier à partir de l'état réel du dépôt. **Vérification :** les deux
adresses ont été ouvertes et les deux hash relus sur GitHub après le push final, jamais recopiés
d'un état antérieur.

**Ce que je referais autrement avec une journée de plus :** j'aurais mis le **budget de
d'exploration de la migration H2** en face du budget de livraison. J'ai passé quarante minutes à
découvrir que la base de test ne sait pas supprimer un index de contrainte, alors que le sujet
demande que « les migrations fonctionnent avec PostgreSQL en exécution et H2 en test ». Un `DROP
CONSTRAINT` simple qui passe sur PostgreSQL ne prouve rien ici : il fallait le découvrir au début,
pas à la fin. Mieux : lire `application-test.yml` avant d'écrire la première migration, et écrire
un test de migration — « cette migration passe-t-elle sur les deux bases ? » — avant d'écrire le
code métier qui en dépend.
