package com.kfokam48.presencerelecture.relecture.api;

/**
 * Note retenue par l'auteur pour un exercice (EF17, RG20).
 *
 * <p>La moyenne des notes rendues est calculée ici, par l'API : le frontend ne la recalcule
 * jamais (F3). `provisoire` dit si cette valeur est encore susceptible de changer.
 */
public record ReceivedReviewResponse(
        Long exerciceId,
        Long sessionId,
        String lienExercice,
        Double note,
        String commentaire,
        Integer nbNotes,
        boolean provisoire
) {
}
