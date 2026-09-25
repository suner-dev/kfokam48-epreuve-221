package com.kfokam48.presencerelecture.relecture.api;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Integer note,
        String commentaire,
        Instant rendueAt
) {
}
