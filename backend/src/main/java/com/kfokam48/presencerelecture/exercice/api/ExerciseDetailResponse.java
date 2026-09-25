package com.kfokam48.presencerelecture.exercice.api;

import com.kfokam48.presencerelecture.relecture.api.ReviewAssignmentResponse;
import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;

import java.time.Instant;
import java.util.List;

/**
 * Détail d'un exercice pour le formateur : chaque affectation est conservée séparément
 * (deux depuis l'enveloppe de l'étape 3) et la note retenue est agrégée par l'API.
 */
public record ExerciseDetailResponse(
        Long id,
        Long etudiantId,
        StatutExercice statut,
        List<ReviewAssignmentResponse> relecteurs,
        Long relecteurId,
        Long relectureId,
        Instant commenceeAt,
        Instant rendueAt,
        Double noteRetenue,
        boolean provisoire
) {

    /**
     * Les quatre champs singuliers sont conservés par sécurité de contrat : ils reflètent
     * la **première** affectation, celle qui portait le sens avant l'enveloppe. Le formateur,
     * seul acteur autorisé à voir l'identité du relecteur (Q8), doit pouvoir lire l'écran
     * d'avant comme celui d'après sans rupture ; la vérité reste dans {@code relecteurs}.
     */
    public static ExerciseDetailResponse of(
            Long id,
            Long etudiantId,
            StatutExercice statut,
            List<ReviewAssignmentResponse> relecteurs,
            Double noteRetenue,
            boolean provisoire
    ) {
        ReviewAssignmentResponse premiere = relecteurs.isEmpty() ? null : relecteurs.get(0);
        return new ExerciseDetailResponse(
                id,
                etudiantId,
                statut,
                relecteurs,
                premiere == null ? null : premiere.relecteurId(),
                premiere == null ? null : premiere.relectureId(),
                premiere == null ? null : premiere.commenceeAt(),
                premiere == null ? null : premiere.rendueAt(),
                noteRetenue,
                provisoire
        );
    }
}
