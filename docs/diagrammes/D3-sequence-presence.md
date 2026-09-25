# D3 — Diagramme de séquence : « marquer sa présence »

> **Contrat de cohérence (barème : D3 ↔ codes HTTP du contrat).** Le cas nominal et les refus
> métiers utilisent exactement les statuts et le format d’erreur de `api/contrat.yaml` pour
> `POST /api/presences`. L’ordre implémenté est : code inconnu, clôture, expiration, fin, étudiant
> inconnu, doublon. Couche `@RestControllerAdvice` = point unique de production des erreurs (**B4**).

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (Angular)
    participant C as PresenceController
    participant S as PresenceService
    participant DB as Repository / BDD
    participant A as RestControllerAdvice

    E->>F: choisit son nom + saisit le code
    F->>C: POST /api/presences { code, etudiantId }
    C->>S: enregistrer(code, etudiantId)

    alt code inconnu
        S->>DB: recherche session par code → aucune
        S-->>A: CodeInconnuException
        A-->>F: 400 { code:"CODE_INCONNU", message:"Code de présence inconnu." }
    else session clôturée (RG15)
        S->>DB: vérifie clotureAt en premier
        S-->>A: SessionClotureeException
        A-->>F: 410 { code:"SESSION_CLOTUREE", message:"La session est clôturée." }
    else code expiré (RG1) — now >= expirationAt
        S->>DB: vérifie expirationAt après clôture
        S-->>A: CodeExpireException
        A-->>F: 410 { code:"CODE_EXPIRE", message:"Le code de présence a expiré." }
    else session terminée (RG2/H7)
        S->>DB: vérifie finAt après expiration
        S-->>A: SessionTermineeException
        A-->>F: 410 { code:"SESSION_TERMINEE", message:"La session est terminée." }
    else étudiant inconnu
        S->>DB: vérifie etudiant_id dans la promotion
        S-->>A: EtudiantInconnuException
        A-->>F: 400 { code:"ETUDIANT_INCONNU", message:"L'étudiant demandé n'existe pas." }
    else étudiant déjà présent (RG3)
        S->>DB: SELECT presence(session_id, etudiant_id) → existe
        S-->>A: DejaPresentException
        A-->>F: 409 { code:"DEJA_PRESENT", message:"Vous êtes déjà présent pour cette session." }
    else cas nominal — code valide, avant clôture, expiration et fin, étudiant valide et jamais présent
        S->>DB: INSERT presence(source=ETUDIANT, marquee_at=now)
        DB-->>S: presence(id)
        S-->>C: Presence(id, sessionId, etudiantId, ETUDIANT)
        C-->>F: 201 { id, sessionId, etudiantId, source:"ETUDIANT" }
        F-->>E: « Présence enregistrée »
    end

    Note over C,A: Les exceptions sont transformées par @RestControllerAdvice (B4)<br/>en { code, message }, jamais en stack trace.
```

**Vérification de conformité au contrat (`POST /api/presences`) :**

| Situation du diagramme | Code HTTP | `code` d'erreur | Conforme annexe B ? |
|---|---|---|---|
| Nominal | `201` + `{id, sessionId, etudiantId, source}` | — | ✅ |
| Code inconnu | `400` | `CODE_INCONNU` | ✅ |
| Session clôturée | `410` | `SESSION_CLOTUREE` | ✅ |
| Code expiré (RG1) | `410` | `CODE_EXPIRE` | ✅ |
| Session terminée (RG2/H7) | `410` | `SESSION_TERMINEE` | ✅ |
| Étudiant inconnu | `400` | `ETUDIANT_INCONNU` | ✅ |
| Déjà présent (RG3) | `409` | `DEJA_PRESENT` | ✅ |

> La règle anti-devinte H5 conserve `400 TROP_ESSAIS` à partir de la cinquième saisie invalide ;
> elle n'est pas représentée ici pour laisser visibles les trois cas d'erreur explicitement exigés
> par le sujet.
