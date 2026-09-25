package com.kfokam48.presencerelecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.relecture.api.ReviewResponse;
import com.kfokam48.presencerelecture.relecture.api.SubmitReviewRequest;
import com.kfokam48.presencerelecture.relecture.application.ReviewService;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {
    private final RelectureRepository relectureRepository = mock(RelectureRepository.class);
    private final ExerciceRepository exerciceRepository = mock(ExerciceRepository.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(relectureRepository, exerciceRepository, sessionService, clock);
    }

    @Test
    void accepteLesBornesDeLaNote() {
        Relecture review = reviewWithReviewer(2L);
        Exercice exercise = exerciseWithAuthor(1L);
        configureReview(review, exercise);

        service.submit(9L, new SubmitReviewRequest(BigDecimal.ZERO, "Zéro"));
        service.submit(9L, new SubmitReviewRequest(BigDecimal.valueOf(20), "Vingt"));
        verify(review).render(0, "Zéro", clock.instant());
        verify(review).render(20, "Vingt", clock.instant());
    }

    @Test
    void refuseUneNoteHorsBornes() {
        assertThatThrownBy(() -> service.submit(9L, new SubmitReviewRequest(BigDecimal.valueOf(-1), "Négative")))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("NOTE_INVALIDE");
        assertThatThrownBy(() -> service.submit(9L, new SubmitReviewRequest(BigDecimal.valueOf(21), "Trop")))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("NOTE_INVALIDE");
    }

    @Test
    void refuseUneRelectureSansRelecteur() {
        Relecture review = reviewWithReviewer(null);
        configureReview(review, exerciseWithAuthor(1L));

        assertThatThrownBy(() -> service.submit(9L, new SubmitReviewRequest(BigDecimal.TEN, "Note")))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("RELECTURE_NON_ASSIGNEE");
    }

    @Test
    void refuseLautoRelecture() {
        Relecture review = reviewWithReviewer(1L);
        configureReview(review, exerciseWithAuthor(1L));

        assertThatThrownBy(() -> service.submit(9L, new SubmitReviewRequest(BigDecimal.TEN, "Note")))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("AUTO_RELECTURE");
    }

    private void configureReview(Relecture review, Exercice exercise) {
        when(relectureRepository.findById(9L)).thenReturn(Optional.of(review));
        when(exerciceRepository.findById(8L)).thenReturn(Optional.of(exercise));
        when(sessionService.require(3L)).thenReturn(mock(SessionCours.class));
    }

    private Relecture reviewWithReviewer(Long reviewerId) {
        Relecture review = mock(Relecture.class);
        when(review.getExerciceId()).thenReturn(8L);
        when(review.getRelecteurId()).thenReturn(reviewerId);
        when(review.getRendueAt()).thenReturn(null);
        return review;
    }

    private Exercice exerciseWithAuthor(Long authorId) {
        Exercice exercise = mock(Exercice.class);
        when(exercise.getId()).thenReturn(8L);
        when(exercise.getSessionId()).thenReturn(3L);
        when(exercise.getEtudiantId()).thenReturn(authorId);
        return exercise;
    }
}
