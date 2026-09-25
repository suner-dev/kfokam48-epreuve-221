package com.kfokam48.presencerelecture.relecture.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.relecture.api.ReviewStartResponse;
import com.kfokam48.presencerelecture.relecture.api.ReviewTaskResponse;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewTaskService {
    private final RelectureRepository relectureRepository;
    private final ExerciceRepository exerciceRepository;
    private final EtudiantRepository etudiantRepository;
    private final SessionService sessionService;
    private final Clock clock;

    public ReviewTaskService(
            RelectureRepository relectureRepository,
            ExerciceRepository exerciceRepository,
            EtudiantRepository etudiantRepository,
            SessionService sessionService,
            Clock clock
    ) {
        this.relectureRepository = relectureRepository;
        this.exerciceRepository = exerciceRepository;
        this.etudiantRepository = etudiantRepository;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReviewTaskResponse> findForReviewer(Long reviewerId) {
        if (!etudiantRepository.existsById(reviewerId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "L'étudiant demandé n'existe pas.");
        }
        return relectureRepository.findPendingForReviewer(reviewerId).stream()
                .map(review -> {
                    Exercice exercise = requireExercise(review);
                    return new ReviewTaskResponse(
                            review.getId(), exercise.getId(), exercise.getLien(), review.getCommenceeAt()
                    );
                })
                .toList();
    }

    public ReviewStartResponse start(Long reviewId) {
        Relecture review = relectureRepository.findById(reviewId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "RELECTURE_INCONNUE",
                "La relecture demandée n'existe pas."
        ));
        Exercice exercise = requireExercise(review);
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
        if (review.getCommenceeAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_DEJA_COMMENCEE", "La relecture a déjà été commencée.");
        }
        review.start(clock.instant());
        return new ReviewStartResponse(review.getId(), review.getCommenceeAt());
    }

    private Exercice requireExercise(Relecture review) {
        return exerciceRepository.findById(review.getExerciceId()).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "EXERCICE_INCONNUE",
                "L'exercice associé n'existe pas."
        ));
    }
}
