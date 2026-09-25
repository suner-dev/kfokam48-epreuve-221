package com.kfokam48.presencerelecture.exercice.api;

import com.kfokam48.presencerelecture.exercice.application.ExerciseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercices")
public class ExerciseController {
    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExerciseCreatedResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}
