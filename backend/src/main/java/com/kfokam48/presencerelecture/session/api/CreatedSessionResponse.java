package com.kfokam48.presencerelecture.session.api;

import java.time.Instant;

public record CreatedSessionResponse(
        Long id,
        String code,
        Instant ouvertureAt,
        Instant expirationAt
) {
}
