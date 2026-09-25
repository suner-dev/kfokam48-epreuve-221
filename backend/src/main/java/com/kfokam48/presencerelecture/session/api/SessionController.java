package com.kfokam48.presencerelecture.session.api;

import com.kfokam48.presencerelecture.session.application.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreatedSessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}
