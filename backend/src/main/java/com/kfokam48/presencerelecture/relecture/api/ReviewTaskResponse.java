package com.kfokam48.presencerelecture.relecture.api;

import java.time.Instant;

public record ReviewTaskResponse(
        Long relectureId,
        Long exerciceId,
        String lienExercice,
        Instant commenceeAt
) {
}
