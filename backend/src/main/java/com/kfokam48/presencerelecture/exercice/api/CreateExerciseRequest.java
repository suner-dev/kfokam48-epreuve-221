package com.kfokam48.presencerelecture.exercice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExerciseRequest(
        @NotNull Long sessionId,
        @NotNull Long etudiantId,
        @NotBlank String lien
) {
}
