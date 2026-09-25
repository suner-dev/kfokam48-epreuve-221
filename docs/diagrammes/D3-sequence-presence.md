# D3 — Diagramme de séquence : « marquer sa présence »

> **Contrat de cohérence (barème : D3 ↔ codes HTTP du contrat).** Le cas nominal et **deux cas
> d'erreur** (code expiré, étudiant déjà présent) utilisent **exactement** les statuts et le format
> d'erreur de `api/contrat.yaml` pour `POST /api/presences` : `201` / `410 CODE_EXPIRE` /
> `409 DEJA_PRESENT` (et `400 CODE_INCONNU` en remarque). Couche `@RestControllerAdvice` = point
> unique de production des erreurs (**B4**).

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (Angular)
    participant C as PresenceController
    participant S as PresenceService
    participant DB as Repository / BDD

    E->>F: choisit son nom + saisit le code
    F->>C: POST /api/presences { code, etudiantId }
    C->>S: enregistrer(code, etudiantId)

    alt code inconnu
        S->>DB: recherche session par code → aucune
        S-->>C: CodeInconnuException
        C-->>F: 400 { code:"CODE_INCONNU", message:"Code de présence inconnu." }
    else code expiré (RG1) — now > expirationAt
        S->>DB: session trouvée, expiration_at dépassé
        S-->>C: CodeExpireException
        C-->>F: 410 { code:"CODE_EXPIRE", message:"Le code de présence a expiré." }
    else étudiant déjà présent (RG3) — UNIQUE(session_id, etudiant_id)
        S->>DB: SELECT presence(session_id, etudiant_id) → existe
        S-->>C: DejaPresentException
        C-->>F: 409 { code:"DEJA_PRESENT", message:"Vous êtes déjà présent pour cette session." }
    else cas nominal — code valide, non expiré, jamais présent
        S->>DB: INSERT presence(source=ETUDIANT, marquee_at=now)
        DB-->>S: presence(id)
        S-->>C: Presence(id, sessionId, etudiantId, ETUDIANT)
        C-->>F: 201 { id, sessionId, etudiantId, source:"ETUDIANT" }
        F-->>E: « Présence enregistrée »
    end

    Note over C: Toute exception remonte par @RestControllerAdvice (B4) et<br/>produit le corps { code, message } — jamais une stack trace.
```

**Vérification de conformité au contrat (`POST /api/presences`) :**

| Situation du diagramme | Code HTTP | `code` d'erreur | Conforme annexe B ? |
|---|---|---|---|
| Nominal | `201` + `{id, sessionId, etudiantId, source}` | — | ✅ |
| Code inconnu | `400` | `CODE_INCONNU` | ✅ |
| Code expiré (RG1) | `410` | `CODE_EXPIRE` | ✅ |
| Déjà présent (RG3) | `409` | `DEJA_PRESENT` | ✅ |

> Remarque (hors du champ de D3, traitée en EF12/RG4) : le 5ᵉ échec de saisie déclenche un
> `429 TROP_ESSAIS` (hypothèse **H5** du cahier) — non représenté ici pour ne pas masquer les
> trois cas exigés par le barème.
