package com.kfokam48.presencerelecture.presence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.exercice.application.ExerciseService;
import com.kfokam48.presencerelecture.presence.api.ManualPresenceRequest;
import com.kfokam48.presencerelecture.presence.api.PresenceResponse;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.presence.domain.Presence;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.presence.domain.SourcePresence;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * B6 — présence manuelle du formateur (RG13, RG15) et orchestration du comptage
 * anti-devinette (EF12).
 *
 * <p>Depuis l'issue #67, {@link PresenceService} n'est plus transactionnel : il orchestre
 * l'écriture de la présence et le comptage anti-devinette, qui doivent se succéder et non
 * s'imbriquer, faute de quoi deux connexions du pool sont retenues en même temps.
 */
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {
    private static final Instant OUVERTURE = Instant.parse("2026-09-25T08:00:00Z");
    private static final Instant EXPIRATION = OUVERTURE.plusSeconds(15 * 60);
    private static final String CODE = "ABCDEFGH";
    private static final long ETUDIANT_ID = 1L;
    private static final long PROMOTION_ID = 2L;

    @Mock
    private PresenceRepository repository;

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private SessionService sessionService;

    @Mock
    private EtudiantService etudiantService;

    @Mock
    private CodeAttemptService codeAttemptService;

    @Mock
    private PresenceRecorder recorder;

    @Test
    void enregistreUnePresenceManuelleAvecLaSourceFormateurSansDemanderDeCode() {
        PresenceService service = serviceAt(OUVERTURE.plusSeconds(60));
        when(sessionService.require(1L)).thenReturn(session());
        when(etudiantService.require(ETUDIANT_ID, PROMOTION_ID)).thenReturn(etudiant());
        when(repository.findBySessionIdAndEtudiantId(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(Presence.class))).thenAnswer(call -> call.getArgument(0));

        service.addManually(new ManualPresenceRequest(1L, ETUDIANT_ID));

        ArgumentCaptor<Presence> saved = ArgumentCaptor.forClass(Presence.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(SourcePresence.FORMATEUR);
    }

    @Test
    void leFormateurNePeutAjouterUnePresenceApresLaCloture() {
        PresenceService service = serviceAt(EXPIRATION.plusSeconds(600));
        SessionCours session = session();
        session.close(EXPIRATION, EXPIRATION.plusSeconds(300));
        when(sessionService.require(1L)).thenReturn(session);

        assertErreur(() -> service.addManually(new ManualPresenceRequest(1L, ETUDIANT_ID)),
                HttpStatus.GONE, "SESSION_CLOTUREE");
        verifyNoInteractions(repository);
    }

    @Test
    void compteLEchecMemeQuandLEcritureDePresenceEstAnnulee() {
        PresenceService service = serviceAt(EXPIRATION);
        when(recorder.record(CODE, ETUDIANT_ID))
                .thenThrow(new ApiException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré."));

        assertErreur(() -> service.mark(new MarkPresenceRequest(CODE, ETUDIANT_ID)),
                HttpStatus.GONE, "CODE_EXPIRE");
        verify(codeAttemptService).registerFailure(ETUDIANT_ID);
        verify(codeAttemptService, never()).registerSuccess(any());
    }

    @Test
    void remetLeCompteurAZeroApresUnMarquageReussi() {
        PresenceService service = serviceAt(EXPIRATION.minusSeconds(1));
        PresenceResponse recorded = new PresenceResponse(1L, 1L, ETUDIANT_ID, SourcePresence.ETUDIANT);
        when(recorder.record(CODE, ETUDIANT_ID)).thenReturn(recorded);

        assertThat(service.mark(new MarkPresenceRequest(CODE, ETUDIANT_ID))).isSameAs(recorded);
        verify(codeAttemptService).registerSuccess(ETUDIANT_ID);
        verify(codeAttemptService, never()).registerFailure(any());
    }

    private PresenceService serviceAt(Instant now) {
        return new PresenceService(repository, exerciseService, sessionService, etudiantService, codeAttemptService,
                recorder, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static SessionCours session() {
        return new SessionCours("Session Java", CODE, PROMOTION_ID, OUVERTURE, EXPIRATION);
    }

    private static Etudiant etudiant() {
        return new Etudiant("Dupont", "Alice", PROMOTION_ID);
    }

    private static void assertErreur(Runnable appel, HttpStatus statut, String code) {
        assertThatThrownBy(appel::run)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(statut);
                    assertThat(exception.code()).isEqualTo(code);
                    assertThat(exception.getMessage()).isNotBlank();
                });
    }
}
