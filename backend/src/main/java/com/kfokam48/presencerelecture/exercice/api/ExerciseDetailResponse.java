package com.kfokam48.presencerelecture.exercice.api;

import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;

import java.time.Instant;

public record ExerciseDetailResponse(
        Long id,
        Long etudiantId,
        StatutExercice statut,
        Long relecteurId,
        Long relectureId,
        Instant commenceeAt,
        Instant rendueAt
) {
}
