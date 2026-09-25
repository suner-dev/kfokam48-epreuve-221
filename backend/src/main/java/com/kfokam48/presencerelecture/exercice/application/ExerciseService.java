package com.kfokam48.presencerelecture.exercice.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.exercice.api.ExerciseCreatedResponse;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.net.URI;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExerciseService {
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;
    private final SessionService sessionService;
    private final EtudiantService etudiantService;
    private final Clock clock;

    public ExerciseService(
            ExerciceRepository exerciceRepository,
            RelectureRepository relectureRepository,
            SessionService sessionService,
            EtudiantService etudiantService,
            Clock clock
    ) {
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
        this.sessionService = sessionService;
        this.etudiantService = etudiantService;
        this.clock = clock;
    }

    public ExerciseCreatedResponse create(CreateExerciseRequest request) {
        validateUri(request.lien());
        SessionCours session = sessionService.require(request.sessionId());
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        Etudiant author = etudiantService.require(request.etudiantId(), session.getPromotionId());
        if (exerciceRepository.findBySessionIdAndEtudiantId(session.getId(), author.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE", "Un exercice est déjà déposé pour cette session.");
        }
        Exercice exercise = exerciceRepository.save(new Exercice(
                session.getId(), author.getId(), request.lien(), StatutExercice.EN_ATTENTE_SANS_RELECTEUR, clock.instant()
        ));
        relectureRepository.save(new Relecture(exercise.getId(), null));
        return new ExerciseCreatedResponse(exercise.getId(), exercise.getStatut());
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
