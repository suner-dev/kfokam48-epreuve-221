package com.kfokam48.presencerelecture.exercice.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.exercice.api.ExerciseCreatedResponse;
import com.kfokam48.presencerelecture.exercice.api.ExerciseDetailResponse;
import com.kfokam48.presencerelecture.exercice.api.ReplaceExerciseRequest;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;
import com.kfokam48.presencerelecture.presence.domain.Presence;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExerciseService {
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;
    private final PresenceRepository presenceRepository;
    private final SessionService sessionService;
    private final EtudiantService etudiantService;
    private final Clock clock;

    public ExerciseService(
            ExerciceRepository exerciceRepository,
            RelectureRepository relectureRepository,
            PresenceRepository presenceRepository,
            SessionService sessionService,
            EtudiantService etudiantService,
            Clock clock
    ) {
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
        this.presenceRepository = presenceRepository;
        this.sessionService = sessionService;
        this.etudiantService = etudiantService;
        this.clock = clock;
    }

    public ExerciseCreatedResponse create(CreateExerciseRequest request) {
        validateUri(request.lien());
        SessionCours session = sessionService.require(request.sessionId(), HttpStatus.BAD_REQUEST);
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        Etudiant author = etudiantService.require(request.etudiantId(), session.getPromotionId());
        if (exerciceRepository.findBySessionIdAndEtudiantId(session.getId(), author.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE", "Un exercice est déjà déposé pour cette session.");
        }
        Long reviewerId = chooseReviewer(session.getId(), author.getId());
        StatutExercice statut = reviewerId == null
                ? StatutExercice.EN_ATTENTE_SANS_RELECTEUR
                : StatutExercice.EN_ATTENTE_DE_RELECTURE;
        Exercice exercise = exerciceRepository.save(new Exercice(
                session.getId(), author.getId(), request.lien(), statut, clock.instant()
        ));
        relectureRepository.save(new Relecture(exercise.getId(), reviewerId));
        return new ExerciseCreatedResponse(exercise.getId(), exercise.getStatut());
    }

    public ExerciseCreatedResponse replace(Long id, ReplaceExerciseRequest request) {
        validateUri(request.lien());
        Exercice exercise = exerciceRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "EXERCICE_INCONNU",
                "L'exercice demandé n'existe pas."
        ));
        SessionCours session = sessionService.require(exercise.getSessionId());
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        Relecture review = relectureRepository.findByExerciceId(id).orElse(null);
        if (review != null && review.getCommenceeAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_COMMENCEE", "La relecture a commencé.");
        }
        exercise.replaceLien(request.lien());
        return new ExerciseCreatedResponse(exercise.getId(), exercise.getStatut());
    }

    @Transactional(readOnly = true)
    public List<ExerciseDetailResponse> details(Long sessionId) {
        sessionService.require(sessionId);
        return exerciceRepository.findBySessionIdOrderById(sessionId).stream()
                .map(exercise -> {
                    Relecture review = relectureRepository.findByExerciceId(exercise.getId()).orElse(null);
                    return new ExerciseDetailResponse(
                            exercise.getId(), exercise.getEtudiantId(), exercise.getStatut(),
                            review == null ? null : review.getRelecteurId(),
                            review == null ? null : review.getId(),
                            review == null ? null : review.getCommenceeAt(),
                            review == null ? null : review.getRendueAt()
                    );
                })
                .toList();
    }

    public void assignPending(Long sessionId) {
        for (Exercice exercise : exerciceRepository.findBySessionIdOrderById(sessionId)) {
            Relecture review = relectureRepository.findByExerciceId(exercise.getId()).orElse(null);
            if (review == null || review.getRelecteurId() != null) {
                continue;
            }
            Long reviewerId = chooseReviewer(sessionId, exercise.getEtudiantId());
            if (reviewerId != null) {
                review.assign(reviewerId);
                exercise.setStatut(StatutExercice.EN_ATTENTE_DE_RELECTURE);
            }
        }
    }

    private Long chooseReviewer(Long sessionId, Long authorId) {
        List<Long> candidates = presenceRepository.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiantId)
                .filter(id -> !id.equals(authorId))
                .distinct()
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private void validateUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getScheme() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE", "Le lien doit être une URI valide.");
        }
    }
}
