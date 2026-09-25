package com.kfokam48.presencerelecture.presence.api;

import com.kfokam48.presencerelecture.presence.domain.SourcePresence;

public record PresenceResponse(
        Long id,
        Long sessionId,
        Long etudiantId,
        SourcePresence source
) {
}
