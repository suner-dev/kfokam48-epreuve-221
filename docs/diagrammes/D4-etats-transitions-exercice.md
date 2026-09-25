# D4 — Diagramme états-transitions : cycle de vie d'un exercice *(bonus +3)*

> Cycle de vie demandé par le sujet (déposé → en attente de relecture → relu), complété par les
> états sans relecteur et les états verrouillés par la clôture. Il est aligné avec `Exercice.statut`
> (D2), Q7/Q11 (affectation et attente), Q13 (début de relecture), Q10/RG9 (correction) et H1/H7
> (fin et clôture).

```mermaid
stateDiagram-v2
    [*] --> DEPOSE : EF3 dépôt du lien (201)

    DEPOSE --> EN_ATTENTE_DE_RELECTURE : tirage aléatoire de deux pairs présents distincts<br/>Q7/RG7 + RG20 (enveloppe étape 3)
    DEPOSE --> EN_ATTENTE_SANS_RELECTEUR : moins de deux pairs éligibles présents<br/>H3 (relecteurId null) — un seul pair disponible : une seule affectation
    EN_ATTENTE_SANS_RELECTEUR --> EN_ATTENTE_DE_RELECTURE : un pair devient éligible<br/>ré-attribution lors d'une nouvelle présence, jusqu'à deux

    EN_ATTENTE_DE_RELECTURE --> EN_ATTENTE_DE_RELECTURE : EF9/RG12 remplacement du lien<br/>tant que commenceeAt est null
    EN_ATTENTE_DE_RELECTURE --> EN_ATTENTE_DE_RELECTURE : EF16/RG18 début de relecture<br/>le lien devient non remplaçable
    EN_ATTENTE_DE_RELECTURE --> RELU : EF5 relecture rendue<br/>note 0..20 + commentaire, RG8<br/>**EF17/RG20 : la note retenue est la moyenne des deux ;**<br/>**provisoire tant qu'un seul des deux pairs a rendu**

    RELU --> RELU : EF10/RG9 correction de la note<br/>session non clôturée, exercice toujours RELU
    RELU --> RELU_VERROUILLE : clôture de session<br/>lecture seule

    EN_ATTENTE_DE_RELECTURE --> EN_ATTENTE_VERROUILLE : clôture de session<br/>relecture encore due, visible tableau (Q11)
    EN_ATTENTE_SANS_RELECTEUR --> EN_ATTENTE_VERROUILLE : clôture de session<br/>toujours sans pair

    RELU_VERROUILLE --> [*]

    note right of EN_ATTENTE_SANS_RELECTEUR
        Visible au formateur via le détail des exercices (Q11)
        et ne compte dans relecturesEnAttente d'aucun étudiant.
    end note
    note right of EN_ATTENTE_VERROUILLE
        Exercice en attente gelé à la clôture (H1/RG15).
    end note
    note right of RELU_VERROUILLE
        Note figée à la clôture (H1/RG15).
        La correction Q10/RG9 n'est plus possible.
    end note
```

**Transitions ↔ exigences/règles :**

| Transition | Déclencheur | Réf |
|---|---|---|
| `[*] → DEPOSE` | `POST /api/exercices` (lien valide, non déjà déposé) | EF3 / RG16 |
| `DEPOSE → EN_ATTENTE_DE_RELECTURE` | assignation système d'un relecteur | EF4 / RG7 |
| `DEPOSE → EN_ATTENTE_SANS_RELECTEUR` | aucun pair présent ≠ auteur | H3 |
| `EN_ATTENTE_SANS_RELECTEUR → EN_ATTENTE_DE_RELECTURE` | nouvelle présence éligible et réévaluation des exercices sans relecteur | H3 / H9 |
| `EN_ATTENTE → EN_ATTENTE` | remplacement du lien avant `commenceeAt`, puis début de relecture | EF9 / EF16 / RG12 / RG18 |
| `EN_ATTENTE → RELU` | rendu de la note | EF5 / RG8 |
| `RELU → RELU` | correction avant clôture ; l'exercice ne repasse pas en attente | EF10 / RG9 / Q10 |
| `EN_ATTENTE → EN_ATTENTE_VERROUILLE` | clôture d'un exercice encore en attente | EF11 / RG15 / H1 |
| `RELU → RELU_VERROUILLE` | clôture d'un exercice relu | EF11 / RG15 / H1 |

> Le statut rendu par `POST /api/exercices` vaut `EN_ATTENTE_DE_RELECTURE` lorsqu'un pair est
> attribué, `EN_ATTENTE_SANS_RELECTEUR` sinon ; la clôture transforme les exercices encore en
> attente en `EN_ATTENTE_VERROUILLE` et les exercices relus en `RELU_VERROUILLE`. La fin seule
> (`finAt`) ne change pas le statut de l'exercice : elle bloque seulement l'auto-marquage.

---

## Révision 2.0 — après l'enveloppe de l'étape 3 (25/09/2026)

Ce diagramme **devient faux** : l'état `RELU` n'était atteignable qu'après **une** relecture rendue.
Il l'est désormais après la première des deux, mais la note affichée est alors **provisoire**.

| Ce qui change | Avant | Après |
|---|---|---|
| Assignation au dépôt | 1 pair | **2 pairs distincts** (ou 1, ou 0 si moins de candidats éligibles) |
| Passage à `RELU` | 1re note rendue | 1re **ou** 2e note rendue |
| Note retenue | la note du relecteur | **moyenne des notes rendues** |
| Mention provisoire | inexistante | `provisoire = true` tant que `nbNotes < 2` |

Le passage à `RELU` reste le même événement observable : c'est la **valeur** de la note qui devient
provisoire, pas l'état de l'exercice. C'est pourquoi aucun nouvel état n'a été introduit — un
`RELU_PROVISOIRE` aurait fait dépendre l'état d'un compteur, alors que la donnée existe déjà dans
la réponse de l'API.
