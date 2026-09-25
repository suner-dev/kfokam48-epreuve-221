package com.kfokam48.presencerelecture.exercice.api;

import com.kfokam48.presencerelecture.exercice.application.ExerciseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExerciseController {
    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @PostMapping("/exercices")
    public ResponseEntity<ExerciseCreatedResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/exercices/{id}")
    public ExerciseCreatedResponse replace(@PathVariable Long id, @Valid @RequestBody ReplaceExerciseRequest request) {
        return service.replace(id, request);
    }

    @GetMapping("/sessions/{id}/exercices")
    public List<ExerciseDetailResponse> details(@PathVariable Long id) {
        return service.details(id);
    }
}
