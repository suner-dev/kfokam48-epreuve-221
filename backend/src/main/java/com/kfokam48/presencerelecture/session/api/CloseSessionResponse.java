package com.kfokam48.presencerelecture.session.api;

import java.time.Instant;

public record CloseSessionResponse(Long id, Instant finAt, Instant clotureAt) {
}
