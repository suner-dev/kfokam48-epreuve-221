package com.kfokam48.presencerelecture.relecture.application;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {
    private final RelectureRepository relectureRepository;
    private final ExerciceRepository exerciceRepository;
    private final SessionService sessionService;
    private final Clock clock;

    public ReviewService(
            RelectureRepository relectureRepository,
            ExerciceRepository exerciceRepository,
            SessionService sessionService,
            Clock clock
    ) {
        this.relectureRepository = relectureRepository;
        this.exerciceRepository = exerciceRepository;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    public ReviewResponse submit(Long id, SubmitReviewRequest request) {
        int note = validateNote(request.note());
        Relecture review = relectureRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "RELECTURE_INCONNUE",
                "La relecture demandée n'existe pas."
        ));
        Exercice exercise = exerciceRepository.findById(review.getExerciceId()).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "EXERCICE_INCONNU",
                "L'exercice associé n'existe pas."
        ));
        SessionCours session = sessionService.require(exercise.getSessionId());
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        if (review.getRelecteurId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_NON_ASSIGNEE", "La relecture n'a pas de relecteur.");
        }
        if (review.getRelecteurId().equals(exercise.getEtudiantId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTO_RELECTURE", "Un étudiant ne peut pas relire son propre exercice.");
        }
        if (review.getRendueAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE", "La relecture a déjà été rendue.");
        }
        Instant maintenant = clock.instant();
        if (review.getCommenceeAt() == null) {
            review.start(maintenant);
        }
        review.render(note, request.commentaire(), maintenant);
        exercise.setStatut(StatutExercice.RELU);
        return new ReviewResponse(review.getId(), review.getNote(), review.getCommentaire(), review.getRendueAt());
    }

    private int validateNote(BigDecimal note) {
        if (note == null
                || note.compareTo(BigDecimal.ZERO) < 0
                || note.compareTo(BigDecimal.valueOf(20)) > 0
                || note.stripTrailingZeros().scale() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOTE_INVALIDE", "La note doit être un entier compris entre 0 et 20.");
        }
        return note.intValue();
    }
}
