package com.kfokam48.presencerelecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.exercice.application.ExerciseService;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.presence.application.PresenceService;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.promotion.application.PromotionService;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import com.kfokam48.presencerelecture.session.domain.SessionCoursRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SessionServiceTest {
    @Test
    void refuseUnCodeExpireAuMomentDeLaValidite() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        SessionCours session = new SessionCours("Session", "CODE", 1L, now, now);
        SessionCoursRepository sessionRepository = mock(SessionCoursRepository.class);
        when(sessionRepository.findByCode("CODE")).thenReturn(Optional.of(session));
        SessionService sessionService = new SessionService(
                sessionRepository, mock(PromotionService.class), Clock.fixed(now, ZoneOffset.UTC)
        );
        PresenceService presenceService = new PresenceService(
                mock(PresenceRepository.class), mock(ExerciseService.class), sessionService,
                mock(EtudiantService.class), Clock.fixed(now, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> presenceService.mark(new MarkPresenceRequest("CODE", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("CODE_EXPIRE");
    }
}
