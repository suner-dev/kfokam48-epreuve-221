package com.kfokam48.presencerelecture.session.api;

import java.time.Instant;

public record SessionDetailResponse(
        Long id,
        String titre,
        String code,
        Long promotionId,
        Instant ouvertureAt,
        Instant expirationAt,
        Instant finAt,
        Instant clotureAt
) {
}
