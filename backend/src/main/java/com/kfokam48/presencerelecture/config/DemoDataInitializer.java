package com.kfokam48.presencerelecture.config;

import com.kfokam48.presencerelecture.session.domain.SessionCours;
import com.kfokam48.presencerelecture.session.domain.SessionCoursRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer {
    private final SessionCoursRepository repository;
    private final Clock clock;

    public DemoDataInitializer(SessionCoursRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void refreshDemoSession() {
        repository.findByCode("DEMO2026")
                .filter(session -> session.getFinAt() == null && session.getClotureAt() == null)
                .ifPresent(this::refresh);
    }

    private void refresh(SessionCours session) {
        Instant now = clock.instant();
        session.refreshWindow(now, now.plus(15, ChronoUnit.MINUTES));
    }
}
