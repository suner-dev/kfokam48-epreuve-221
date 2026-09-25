package com.kfokam48.presencerelecture.exercice.api;

import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;

public record ExerciseDetailResponse(
        Long id,
        Long etudiantId,
        StatutExercice statut,
        Long relecteurId,
        Long relectureId
) {
}
