package com.kfokam48.presencerelecture.relecture.api;

public record ReceivedReviewResponse(
        Long exerciceId,
        Long sessionId,
        String lienExercice,
        Integer note,
        String commentaire
) {
}
