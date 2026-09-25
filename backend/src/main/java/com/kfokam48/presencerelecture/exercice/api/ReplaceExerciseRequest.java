package com.kfokam48.presencerelecture.exercice.api;

import jakarta.validation.constraints.NotBlank;

public record ReplaceExerciseRequest(
        @NotBlank String lien
) {
}
