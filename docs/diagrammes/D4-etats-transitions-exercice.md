# D4 — Diagramme états-transitions : cycle de vie d'un exercice *(bonus +3)*

> Cycle de vie demandé par le sujet (déposé → en attente de relecture → relu), mis en cohérence
> avec `Exercice.statut` (D2) et les règles Q11 (en attente visible), Q13 (remplacement du lien),
> Q10/RG9 (correction avant clôture).

```mermaid
stateDiagram-v2
    [*] --> DEPOSE : EF3 dépôt du lien (201)

    DEPOSE --> EN_ATTENTE_DE_RELECTURE : tirage aléatoire d'un pair présent<br/>Q7/RG7 (relecteur assigné)
    DEPOSE --> EN_ATTENTE_SANS_RELECTEUR : aucun pair éligible présent<br/>H3 (relecteurId null)
    EN_ATTENTE_SANS_RELECTEUR --> EN_ATTENTE_DE_RELECTURE : un pair devient éligible<br/→ ré-attribution

    EN_ATTENTE_DE_RELECTURE --> EN_ATTENTE_DE_RELECTURE : EF9/RG12 remplacement du lien<br/>(tant que note non rendue)
    EN_ATTENTE_DE_RELECTURE --> RELU : EF5 relecture rendue<br/>(note 0..20 + commentaire, RG8)

    RELU --> EN_ATTENTE_DE_RELECTURE : EF10/RG9 correction de la note<br/>(session non clôturée, PUT)
    RELU --> RELU_verrouille : POST cloture (H1/RG15)<br/>ou relecture définitivement figée

    EN_ATTENTE_DE_RELECTURE --> RELU_verrouille : clôture de session<br/>avec relecture toujours due<br/→ visible tableau (Q11)

    RELU_verrouille --> [*]

    note right of EN_ATTENTE_SANS_RELECTEUR
        Visible au formateur via
        relecturesEnAttente (Q11)
    end note
    note right of RELU_verrouille
        Après clotureAt : lecture seule (RG15).
        La correction (Q10) n'est plus possible.
    end note
```

**Transitions ↔ exigences/règles :**

| Transition | Déclencheur | Réf |
|---|---|---|
| `[*] → DEPOSE` | `POST /api/exercices` (lien valide, non déjà déposé) | EF3 / RG16 |
| `DEPOSE → EN_ATTENTE_DE_RELECTURE` | assignation système d'un relecteur | EF4 / RG7 |
| `DEPOSE → EN_ATTENTE_SANS_RELECTEUR` | aucun pair présent ≠ auteur | H3 (trou repéré) |
| `EN_ATTENTE → EN_ATTENTE` (auto) | remplacement du lien avant notation | EF9 / RG12 |
| `EN_ATTENTE → RELU` | rendu de la note | EF5 / RG8 |
| `RELU → EN_ATTENTE` (retour) | correction avant clôture | EF10 / RG9 (arbitrage Q10) |
| `* → RELU_verrouille` | `POST /api/sessions/{id}/cloture` | EF11 / RG15 (trou H1) |

> Le statut rendu par `POST /api/exercices` (contrat : `{id, statut}`) vaut
> **`EN_ATTENTE_DE_RELECTURE`** dès qu'un relecteur est assigné, **`DEPOSE`** sinon — c'est le
> seul point de contact entre cette machine à états et le contrat imposé.
