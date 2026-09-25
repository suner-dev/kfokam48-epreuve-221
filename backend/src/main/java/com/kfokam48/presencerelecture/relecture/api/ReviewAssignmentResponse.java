package com.kfokam48.presencerelecture.relecture.api;

import java.time.Instant;

/**
 * Une affectation de relecture, telle que le formateur la voit (Q8 : le seul acteur
 * autorisé à connaître l'identité du relecteur). Depuis l'enveloppe de l'étape 3, un
 * exercice en porte jusqu'à deux.
 */
public record ReviewAssignmentResponse(
        Long relectureId,
        Long relecteurId,
        Instant commenceeAt,
        Instant rendueAt
) {
}
