# D2 — Diagramme de classes / modèle de données

> **Contrat de cohérence (barème : D2 ↔ migrations).** Chaque classe ci-dessous correspond à une
> **table** de la migration Flyway `V1__init.sql`, avec ses colonnes et ses clés. Les cardinalités
> sont celles effectives (contraintes d'unicité / NOT NULL). Types = types persistés ; les `id`
> sont des `BIGINT` (contrat : `int64`).

```mermaid
classDiagram
    class Promotion {
        +BIGINT id
        +String nom
    }
    class Etudiant {
        +BIGINT id
        +String nom
        +String prenom
        +BIGINT promotionId  «FK»
    }
    class Formateur {
        +BIGINT id
        +String nom
    }
    class Session {
        +BIGINT id
        +String titre
        +String code
        +BIGINT promotionId  «FK»
        +BIGINT formateurId  «FK, nullable»
        +Instant ouvertureAt
        +Instant expirationAt   «= ouvertureAt + 15 min (RG1)»
        +Instant clotureAt      «nullable (H1/RG15)»
    }
    class Presence {
        +BIGINT id
        +BIGINT sessionId  «FK»
        +BIGINT etudiantId «FK»
        +SourceEnum source  «ETUDIANT | FORMATEUR (RG13)»
        +Instant marqueeAt
    }
    class Exercice {
        +BIGINT id
        +BIGINT sessionId  «FK»
        +BIGINT etudiantId «FK (auteur)»
        +String lien      «URI (RG: format uri)»
        +StatutEnum statut «DEPOSE | EN_ATTENTE_DE_RELECTURE | RELU (D4)»
        +Instant deposeAt
    }
    class Relecture {
        +BIGINT id        «= {id} de POST /api/relectures/{id} (H2)»
        +BIGINT exerciceId «FK, UNIQUE (RG6 un seul relecteur)»
        +BIGINT relecteurId «FK Etudiant, nullable (H3 si aucun pair)»
        +Integer note      «nullable, 0..20 entier (RG8)»
        +String commentaire «nullable»
        +Instant rendueAt  «nullable; null = en attente (RG10/Q11)»
    }
    class CodeTentative {
        +BIGINT id
        +BIGINT etudiantId «FK»
        +BIGINT sessionId  «FK»
        +int tentatives    «>=5 => blocage (RG4/Q4)»
        +Instant bloqueJusqua «nullable»
    }

    class SourceEnum {
        <<enumeration>>
        ETUDIANT
        FORMATEUR
    }
    class StatutEnum {
        <<enumeration>>
        DEPOSE
        EN_ATTENTE_DE_RELECTURE
        RELU
    }

    Promotion "1" --> "0..*" Etudiant : regroupe
    Promotion "1" --> "0..*" Session : accueille
    Formateur "0..1" --> "0..*" Session : ouvre
    Etudiant "1" --> "0..*" Presence : marque
    Session "1" --> "0..*" Presence : contient
    Session "1" --> "0..*" Exercice : reçoit
    Etudiant "1" --> "0..*" Exercice : dépose (auteur)
    Exercice "1" --> "0..1" Relecture : est relue (RG6)
    Etudiant "1" --> "0..*" Relecture : effectue (relecteur)
    Etudiant "1" --> "0..*" CodeTentative : anti-devinette
    Presence ..> SourceEnum
    Exercice ..> StatutEnum
```

## Correspondance tables / contraintes (ce que produira `V1__init.sql`)

| Classe | Table | Contraintes clés (à retrouver dans la migration) |
|---|---|---|
| `Promotion` | `promotion` | `id PK` |
| `Etudiant` | `etudiant` | `id PK`, `promotion_id FK → promotion` |
| `Formateur` | `formateur` | `id PK` (entité légère ; sert à `session.formateur_id`) |
| `Session` | `session` | `id PK`, `promotion_id FK`, `formateur_id FK NULL`, **`code UNIQUE`**, `expiration_at = ouverture_at + interval '15 minutes'` (calculé au service, RG1) |
| `Presence` | `presence` | `id PK`, `session_id FK`, `etudiant_id FK`, `source IN ('ETUDIANT','FORMATEUR')`, **`UNIQUE(session_id, etudiant_id)`** (RG3 → 409) |
| `Exercice` | `exercice` | `id PK`, `session_id FK`, `etudiant_id FK`, `lien`, **`UNIQUE(session_id, etudiant_id)`** (RG16 → 409 EXERCICE_DEJA_DEPOSE), `statut IN (...)` |
| `Relecture` | `relecture` | `id PK`, **`exercice_id UNIQUE FK`** (RG6), `relecteur_id FK NULL` (H3), `note SMALLINT CHECK (note BETWEEN 0 AND 20)` (RG8 → 400), `rendue_at NULL = en attente` (RG10) |
| `CodeTentative` | `code_tentative` | `id PK`, `UNIQUE(etudiant_id, session_id)`, `tentatives INT`, `bloque_jusqua TIMESTAMP NULL` (RG4) |

**Notes de modélisation (renvois au cahier §7) :**
- `Relecture.relecteur_id` **nullable** = le trou **H3** : exercice en attente sans pair éligible.
- `Session.cloture_at` **nullable** = le trou **H1** (clôture) : `NULL` = session encore vivante.
- La **moyenne** affichée au tableau n'est **pas un attribut** stocké : elle est **calculée côté
  API** (`AVG(relecture.note)` par étudiant) et **jamais recalculée** par le frontend (**F3**).
- `expiration_at` est **matérialisé** (plutôt que recalculé à chaque lecture) pour rendre `410`
  testable sans horloge externe ; il reste dérivé de `ouverture_at` à l'écriture (RG1).
