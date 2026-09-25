package com.kfokam48.presencerelecture.presence.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "presence")
public class Presence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sessionId;
    private Long etudiantId;
    @Enumerated(EnumType.STRING)
    private SourcePresence source;
    private Instant marqueeAt;

    protected Presence() {
    }

    public Presence(Long sessionId, Long etudiantId, SourcePresence source, Instant marqueeAt) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.source = source;
        this.marqueeAt = marqueeAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public SourcePresence getSource() {
        return source;
    }

    public Instant getMarqueeAt() {
        return marqueeAt;
    }
}
