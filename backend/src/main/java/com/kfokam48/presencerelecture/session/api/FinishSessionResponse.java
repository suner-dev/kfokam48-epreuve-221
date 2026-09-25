package com.kfokam48.presencerelecture.session.api;

import java.time.Instant;

public record FinishSessionResponse(Long id, Instant finAt) {
}
