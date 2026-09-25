package com.kfokam48.presencerelecture.presence.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarkPresenceRequest(
        @NotBlank String code,
        @NotNull Long etudiantId
) {
}
