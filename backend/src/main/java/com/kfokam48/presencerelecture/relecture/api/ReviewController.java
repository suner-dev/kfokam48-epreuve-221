package com.kfokam48.presencerelecture.relecture.api;

import com.kfokam48.presencerelecture.relecture.application.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relectures")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/{id}")
    public ResponseEntity<ReviewResponse> submit(
            @PathVariable Long id,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        return ResponseEntity.ok(service.submit(id, request));
    }
}
