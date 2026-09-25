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
 * B6 — Test unitaire métier (consigne 13.1) : les règles d'écriture d'une présence sont
 * démontrées sur la borne exacte, sans contexte Spring, sans base et sans appel HTTP.
 * RG1 expiration, RG2 fin de session, RG3 doublon, RG15 clôture.
 *
 * <p>Ces règles sont portées par {@link PresenceRecorder} depuis l'issue #67 : c'est lui qui
 * tient la transaction de présence, et il ne doit en tenir qu'une seule.
 */
@ExtendWith(MockitoExtension.class)
class PresenceRecorderTest {
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


    @Test
    void marqueLaPresenceALaDerniereSecondeAvantLexpiration() {
        PresenceRecorder recorder = recorderAt(EXPIRATION.minusSeconds(1));
        SessionCours session = session();
        when(sessionService.requireByCode(CODE)).thenReturn(session);
        when(etudiantService.require(ETUDIANT_ID, PROMOTION_ID)).thenReturn(etudiant());
        when(repository.findBySessionIdAndEtudiantId(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(Presence.class))).thenAnswer(call -> call.getArgument(0));

        recorder.record(CODE, ETUDIANT_ID);

        ArgumentCaptor<Presence> saved = ArgumentCaptor.forClass(Presence.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(SourcePresence.ETUDIANT);
        assertThat(saved.getValue().getMarqueeAt()).isEqualTo(EXPIRATION.minusSeconds(1));
        verify(exerciseService).assignPending(session.getId());
    }

    @Test
    void refuseLeCodeAlinstantExactOuIlExpire() {
        PresenceRecorder recorder = recorderAt(EXPIRATION);
        when(sessionService.requireByCode(CODE)).thenReturn(session());

        assertErreur(() -> recorder.record(CODE, ETUDIANT_ID),
                HttpStatus.GONE, "CODE_EXPIRE");
        verifyNoInteractions(repository, exerciseService);
    }

    @Test
    void refuseLeCodeUneFoisLeDelaiDepasse() {
        PresenceRecorder recorder = recorderAt(EXPIRATION.plusSeconds(1));
        when(sessionService.requireByCode(CODE)).thenReturn(session());

        assertErreur(() -> recorder.record(CODE, ETUDIANT_ID),
                HttpStatus.GONE, "CODE_EXPIRE");
    }

    @Test
    void refuseLAutoMarquageDesQueLaSessionEstTermineeAlorsQueLeCodeCourtEncore() {
        PresenceRecorder recorder = recorderAt(EXPIRATION.minusSeconds(60));
        SessionCours session = session();
        session.finish(EXPIRATION.minusSeconds(120));
        when(sessionService.requireByCode(CODE)).thenReturn(session);

        assertErreur(() -> recorder.record(CODE, ETUDIANT_ID),
                HttpStatus.GONE, "SESSION_TERMINEE");
    }

    @Test
    void laClotureEstPrioritaireSurLexpirationEtSurLaFin() {
        // Arbitrage §7 : une session à la fois expirée, terminée et clôturée renvoie SESSION_CLOTUREE.
        PresenceRecorder recorder = recorderAt(EXPIRATION.plusSeconds(60));
        SessionCours session = session();
        session.close(EXPIRATION.minusSeconds(120), EXPIRATION.plusSeconds(30));
        when(sessionService.requireByCode(CODE)).thenReturn(session);

        assertErreur(() -> recorder.record(CODE, ETUDIANT_ID),
                HttpStatus.GONE, "SESSION_CLOTUREE");
    }

    @Test
    void refuseUnePresenceDejaEnregistreePourLaMemeSession() {
        PresenceRecorder recorder = recorderAt(OUVERTURE.plusSeconds(60));
        when(sessionService.requireByCode(CODE)).thenReturn(session());
        when(etudiantService.require(ETUDIANT_ID, PROMOTION_ID)).thenReturn(etudiant());
        when(repository.findBySessionIdAndEtudiantId(any(), any()))
                .thenReturn(Optional.of(new Presence(null, null, SourcePresence.ETUDIANT, OUVERTURE)));

        assertErreur(() -> recorder.record(CODE, ETUDIANT_ID),
                HttpStatus.CONFLICT, "DEJA_PRESENT");
        verify(repository, never()).save(any(Presence.class));
    }

    private PresenceRecorder recorderAt(Instant now) {
        return new PresenceRecorder(repository, exerciseService, sessionService, etudiantService,
                Clock.fixed(now, ZoneOffset.UTC));
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
