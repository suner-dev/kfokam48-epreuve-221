package com.kfokam48.presencerelecture.relecture.api;

import java.math.BigDecimal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(
        @NotNull @Min(0) @Max(20) BigDecimal note,
        @NotNull String commentaire
) {
}
