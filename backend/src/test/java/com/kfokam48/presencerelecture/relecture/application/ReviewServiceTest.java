package com.kfokam48.presencerelecture.relecture.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;
import com.kfokam48.presencerelecture.relecture.api.ReviewResponse;
import com.kfokam48.presencerelecture.relecture.api.SubmitReviewRequest;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * B6 — Test unitaire métier (consigne 13.1) : RG8 bornes de la note, RG2/RG5 auto-relecture,
 * RG15 clôture, H3 affectation sans relecteur. Aucune dépendance à Spring ni à la base.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    private static final Instant OUVERTURE = Instant.parse("2026-09-25T08:00:00Z");
    private static final Instant MAINTENANT = OUVERTURE.plusSeconds(3600);
    private static final long RELECTURE_ID = 10L;
    private static final long EXERCICE_ID = 20L;
    private static final long SESSION_ID = 30L;
    private static final long AUTEUR_ID = 40L;
    private static final long RELECTEUR_ID = 41L;

    @Mock
    private RelectureRepository relectureRepository;

    @Mock
    private ExerciceRepository exerciceRepository;

    @Mock
    private SessionService sessionService;

    private ReviewService service() {
        return new ReviewService(relectureRepository, exerciceRepository, sessionService,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "20"})
    void accepteLesNotesExtremesDeLIntervalleAutorise(String note) {
        Relecture review = relecture(RELECTEUR_ID);
        Exercice exercise = exercice(StatutExercice.EN_ATTENTE_DE_RELECTURE);
        when(relectureRepository.findById(RELECTURE_ID)).thenReturn(Optional.of(review));
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercise));
        when(sessionService.require(SESSION_ID)).thenReturn(session());

        ReviewResponse response = service().submit(RELECTURE_ID, new SubmitReviewRequest(new BigDecimal(note), "Bon travail."));

        assertThat(response.note()).isEqualTo(Integer.parseInt(note));
        assertThat(review.getCommentaire()).isEqualTo("Bon travail.");
        assertThat(review.getRendueAt()).isEqualTo(MAINTENANT);
        assertThat(exercise.getStatut()).isEqualTo(StatutExercice.RELU);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "21", "12.5", "0.5", "20.01"})
    void refuseTouteNoteHorsDeLIntervalleOuNonEntiere(String note) {
        assertErreur(() -> service().submit(RELECTURE_ID, new SubmitReviewRequest(new BigDecimal(note), "Sans effet.")),
                HttpStatus.BAD_REQUEST, "NOTE_INVALIDE");
        // La note est contrôlée avant la moindre lecture : aucune affectation n'est touchée.
        verifyNoInteractions(relectureRepository, exerciceRepository, sessionService);
    }

    @Test
    void refuseUneNoteAbsente() {
        assertErreur(() -> service().submit(RELECTURE_ID, new SubmitReviewRequest(null, "Sans effet.")),
                HttpStatus.BAD_REQUEST, "NOTE_INVALIDE");
        verifyNoInteractions(relectureRepository, exerciceRepository, sessionService);
    }

    @Test
    void refuseQuUnEtudiantReliseSonPropreExercice() {
        Relecture review = relecture(AUTEUR_ID);
        when(relectureRepository.findById(RELECTURE_ID)).thenReturn(Optional.of(review));
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercice(StatutExercice.EN_ATTENTE_DE_RELECTURE)));
        when(sessionService.require(SESSION_ID)).thenReturn(session());

        assertErreur(() -> service().submit(RELECTURE_ID, requete("14")),
                HttpStatus.FORBIDDEN, "AUTO_RELECTURE");
        assertThat(review.getRendueAt()).isNull();
    }

    @Test
    void refuseUneRelectureSansRelecteurDesignE() {
        // Hypothèse H3 : un exercice sans pair est « en attente », sa relecture ne peut être rendue.
        Relecture review = relecture(null);
        when(relectureRepository.findById(RELECTURE_ID)).thenReturn(Optional.of(review));
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercice(StatutExercice.EN_ATTENTE_SANS_RELECTEUR)));
        when(sessionService.require(SESSION_ID)).thenReturn(session());

        assertErreur(() -> service().submit(RELECTURE_ID, requete("14")),
                HttpStatus.CONFLICT, "RELECTURE_NON_ASSIGNEE");
    }

    @Test
    void refuseUnSecondRenduAlorsQueLaSessionNCestPasCloturee() {
        Relecture review = relecture(RELECTEUR_ID);
        review.render(14, "Premier rendu.", MAINTENANT.minusSeconds(60));
        when(relectureRepository.findById(RELECTURE_ID)).thenReturn(Optional.of(review));
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercice(StatutExercice.RELU)));
        when(sessionService.require(SESSION_ID)).thenReturn(session());

        assertErreur(() -> service().submit(RELECTURE_ID, requete("0")),
                HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE");
        assertThat(review.getNote()).isEqualTo(14);
    }

    @Test
    void refuseToutRenduUneFoisLaSessionCloturee() {
        SessionCours cloturee = session();
        cloturee.close(MAINTENANT.minusSeconds(10), MAINTENANT.minusSeconds(1));
        when(relectureRepository.findById(RELECTURE_ID)).thenReturn(Optional.of(relecture(RELECTEUR_ID)));
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercice(StatutExercice.EN_ATTENTE_DE_RELECTURE)));
        when(sessionService.require(SESSION_ID)).thenReturn(cloturee);

        assertErreur(() -> service().submit(RELECTURE_ID, requete("14")),
                HttpStatus.GONE, "SESSION_CLOTUREE");
    }

    @Test
    void repond404QuandLaRelectureDemandeeNExistePas() {
        when(relectureRepository.findById(RELECTURE_ID)).thenReturn(Optional.empty());

        assertErreur(() -> service().submit(RELECTURE_ID, requete("14")),
                HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE");
        verifyNoInteractions(exerciceRepository, sessionService);
    }

    private static SubmitReviewRequest requete(String note) {
        return new SubmitReviewRequest(new BigDecimal(note), "Commentaire de test.");
    }

    private static Relecture relecture(Long relecteurId) {
        return new Relecture(EXERCICE_ID, relecteurId);
    }

    private static Exercice exercice(StatutExercice statut) {
        return new Exercice(SESSION_ID, AUTEUR_ID, "https://example.com/devoir", statut, OUVERTURE);
    }

    private static SessionCours session() {
        return new SessionCours("Session Java", "ABCDEFGH", 1L, OUVERTURE, OUVERTURE.plusSeconds(900));
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
