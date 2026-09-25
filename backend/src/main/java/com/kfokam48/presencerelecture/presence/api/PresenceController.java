package com.kfokam48.presencerelecture.presence.api;

import com.kfokam48.presencerelecture.presence.application.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presences")
public class PresenceController {
    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PresenceResponse> mark(@Valid @RequestBody MarkPresenceRequest request) {
        return ResponseEntity.status(201).body(service.mark(request));
    }

    @PostMapping("/manuelle")
    public ResponseEntity<PresenceResponse> addManually(@Valid @RequestBody ManualPresenceRequest request) {
        return ResponseEntity.status(201).body(service.addManually(request));
    }
}
