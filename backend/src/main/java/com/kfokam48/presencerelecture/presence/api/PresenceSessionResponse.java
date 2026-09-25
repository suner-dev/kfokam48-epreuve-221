package com.kfokam48.presencerelecture.presence.api;

import java.time.Instant;
import com.kfokam48.presencerelecture.presence.domain.SourcePresence;

public record PresenceSessionResponse(
        Long etudiantId,
        String nom,
        boolean present,
        SourcePresence source,
        Instant marqueeAt
) {
}
