# D1 — Diagramme de cas d'utilisation

> Acteurs et ce que chacun peut faire. Le **relecteur** est un **étudiant dans un état donné**
> (cf. cahier §2) : il n'apparaît donc pas comme un acteur séparé mais comme un étudiant qui
> exécute une relecture **assignée**. Formalisme : Mermaid (versionné, diffable).

```mermaid
flowchart LR
    FORMATEUR([Formateur])
    ETUDIANT([Étudiant])
    SYSTEME([Système])

    subgraph App["Application présence & relecture"]
        UC1([Ouvrir une session<br/>→ code de présence])
        UCFIN([Terminer une session<br/>H7 / EF15])
        UCCLOT([Clôturer une session<br/>H1 / RG15])
        UCTAB([Consulter le tableau<br/>Q16 / EF6])
        UCDETAIL([Consulter le détail par session<br/>présences, exercices, sources — Q11/Q14/Q16])
        UCPRES([Marquer sa présence<br/>avec le code])
        UCADD([Ajouter une présence<br/>à la main — source FORMATEUR<br/>Q14 / RG13])
        UCDEP([Déposer le lien<br/>de son exercice])
        UCREP([Remplacer son lien<br/>avant début de relecture — Q13 / RG12])
        UCVOIR([Voir sa note & commentaire<br/>sans identité du relecteur — Q8 / RG14])
        UCLISTE([Choisir son nom dans<br/>une liste — Q1 / EF13])
        UCSTART([Commencer sa relecture<br/>Q13 / H8])
        UCREL([Rendre une relecture<br/>note 0–20 + commentaire])
        UCCORR([Corriger sa relecture<br/>avant clôture — Q10 / RG9])

        UCASSIGN([Assigner un relecteur présent<br/>au hasard — Q7 / RG7])
        UCNOTE(((Note entière<br/>0–20 — RG8)))
        UCAUTE(((Auteur ≠ relecteur<br/>— RG5 / 403)))
        UCEXPI(((Code connu et non expiré<br/>— RG1/RG2)))
        UCNOSTART(((Lien non commencé<br/>— Q13 / H8)))
    end

    FORMATEUR --> UC1
    FORMATEUR --> UCFIN
    FORMATEUR --> UCCLOT
    FORMATEUR --> UCTAB
    FORMATEUR --> UCDETAIL
    FORMATEUR --> UCADD

    ETUDIANT --> UCLISTE
    ETUDIANT --> UCPRES
    ETUDIANT --> UCDEP
    ETUDIANT --> UCREP
    ETUDIANT --> UCVOIR
    ETUDIANT --> UCSTART
    ETUDIANT --> UCREL
    ETUDIANT --> UCCORR

    SYSTEME --> UCASSIGN
    UCDEP -.include.-> UCASSIGN
    UCPRES -.include.-> UCEXPI
    UCREP -.include.-> UCNOSTART
    UCSTART -.include.-> UCNOSTART
    UCREL -.include.-> UCNOTE
    UCREL -.include.-> UCAUTE
    UCCORR -.include.-> UCAUTE
```

**Lecture / renvois :**
- `UC1 → UCPRES → UCDEP → UCASSIGN → UCREL → UCTAB` est le parcours nominal demandé.
- `UCLISTE` est uniquement la sélection d'identité ; `UCASSIGN` est la fonction système déclenchée
  par le dépôt et distincte de l'identité choisie.
- Les doubles parenthèses `((…))` sont des contraintes de gestion ; elles ne constituent pas des
  acteurs ni des extensions de cas d'utilisation.
- `UCFIN` et `UCCLOT` sont deux événements distincts (H7/H1). `UCREP` est bloqué dès `UCSTART`.
- `UCADD` n'existe que pour le formateur (Q14) ; `UCCORR` est borné par la clôture (RG9/RG15).
