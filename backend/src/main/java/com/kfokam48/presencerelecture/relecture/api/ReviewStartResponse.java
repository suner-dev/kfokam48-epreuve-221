package com.kfokam48.presencerelecture.relecture.api;

import java.time.Instant;

public record ReviewStartResponse(
        Long id,
        Instant commenceeAt
) {
}
