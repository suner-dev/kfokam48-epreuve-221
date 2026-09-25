package com.kfokam48.presencerelecture.presence.api;

import jakarta.validation.constraints.NotNull;

public record ManualPresenceRequest(
        @NotNull Long sessionId,
        @NotNull Long etudiantId
) {
}
