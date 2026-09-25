# D1 — Diagramme de cas d'utilisation

> Acteurs et ce que chacun peut faire. Le **relecteur** est un **étudiant dans un état donné**
> (cf. cahier §2) : il n'apparaît donc pas comme un acteur séparé mais comme un étudiant qui
> exécute une relecture **assignée**. Formalisme : Mermaid (versionné, diffable).

```mermaid
flowchart LR
    FORMATEUR([Formateur])
    ETUDIANT([Étudiant])

    subgraph App["Application présence & relecture"]
        UC1([Ouvrir une session<br/>→ code de présence])
        UCCLOT([Clôturer une session<br/>H1 / RG15])
        UCTAB([Consulter le tableau<br/>Q16 / EF6])
        UCPRES([Marquer sa présence<br/>avec le code])
        UCADD([Ajouter une présence<br/>à la main — source FORMATEUR<br/>Q14 / RG13])
        UCDEP([Déposer le lien<br/>de son exercice])
        UCREP([Remplacer son lien<br/>avant relecture — Q13 / RG12])
        UCVOIR([Voir sa note & commentaire<br/>sans le nom du relecteur — Q8 / RG14])
        UCLISTE([Choisir son nom dans<br/>une liste — Q1 / EF13])
        UCREL([Rendre une relecture<br/>note 0–20 + commentaire])
        UCCORR([Corriger sa relecture<br/>avant clôture — Q10 / RG9])

        UCNOTE(((Note entière<br/>0–20 — RG8)))
        UCAUTE(((Auteur ≠ relecteur<br/>— RG5 / 403)))
        UCEXPI(((Code non expiré<br/>— RG1 / 410)))
        UCLIST((Système : tirage au hasard<br/>d'un relecteur présent<br/>Q7 / RG7))
    end

    FORMATEUR --> UC1
    FORMATEUR --> UCCLOT
    FORMATEUR --> UCTAB
    FORMATEUR --> UCADD

    ETUDIANT --> UCLISTE
    ETUDIANT --> UCPRES
    ETUDIANT --> UCDEP
    ETUDIANT --> UCREP
    ETUDIANT --> UCVOIR
    ETUDIANT --> UCREL
    ETUDIANT --> UCCORR

    UCPRES -.include.-> UCEXPI
    UCDEP -.extend.-> UCLISTE
    UCLISTE -.«assigne automatiquement».-  UCREL
    UCREL -.include.-> UCNOTE
    UCREL -.include.-> UCAUTE
    UCCORR -.include.-> UCAUTE

    UCTAB -.voit.-> UCLISTE
```

**Lecture / renvois :**
- `UC1 → UCPRES → UCDEP → UCREL → UCTAB` = le parcours nominal demandé (les 5 points du besoin).
- Les ellipses `((…))` sont des **règles de gestion incluses** (RG1, RG5, RG7, RG8) : un cas
  d'utilisation qui les ignore serait incomplet.
- `UCLISTE` (tirage aléatoire) est une fonction **système**, déclenchée par `UCDEP`, jamais par
  un clic humain : c'est ce qui garantit RG7 et rend l'écran relecteur possible.
- `UCADD` n'existe que pour le formateur (Q14) ; `UCCORR` est borné par la clôture (RG9/RG15).
