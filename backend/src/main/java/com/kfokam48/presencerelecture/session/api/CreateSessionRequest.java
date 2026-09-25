package com.kfokam48.presencerelecture.session.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
        @NotBlank String titre,
        @NotNull Long promotionId
) {
}
