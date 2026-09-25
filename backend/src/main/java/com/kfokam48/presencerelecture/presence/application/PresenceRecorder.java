package com.kfokam48.presencerelecture.presence.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.exercice.application.ExerciseService;
import com.kfokam48.presencerelecture.presence.api.PresenceResponse;
import com.kfokam48.presencerelecture.presence.domain.Presence;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.presence.domain.SourcePresence;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écriture d'une présence, dans une transaction et une seule.
 *
 * <p>Cette classe existe pour que le comptage anti-devinette ne puisse plus être imbriqué dans
 * la transaction de présence (issue #67) : voir {@link PresenceService}.
 */
@Service
@Transactional
class PresenceRecorder {
    private final PresenceRepository repository;
    private final ExerciseService exerciseService;
    private final SessionService sessionService;
    private final EtudiantService etudiantService;
    private final Clock clock;

    PresenceRecorder(
            PresenceRepository repository,
            ExerciseService exerciseService,
            SessionService sessionService,
            EtudiantService etudiantService,
            Clock clock
    ) {
        this.repository = repository;
        this.exerciseService = exerciseService;
        this.sessionService = sessionService;
        this.etudiantService = etudiantService;
        this.clock = clock;
    }

    PresenceResponse record(String code, Long etudiantId) {
        SessionCours session = sessionService.requireByCode(code);
        checkUsable(session);
        Etudiant etudiant = etudiantService.require(etudiantId, session.getPromotionId());
        if (repository.findBySessionIdAndEtudiantId(session.getId(), etudiant.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "DEJA_PRESENT", "La présence est déjà enregistrée pour cette session.");
        }
        Presence presence = repository.save(new Presence(
                session.getId(), etudiant.getId(), SourcePresence.ETUDIANT, clock.instant()
        ));
        exerciseService.assignPending(session.getId());
        return new PresenceResponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(), presence.getSource());
    }

    private void checkUsable(SessionCours session) {
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        if (!clock.instant().isBefore(session.getExpirationAt())) {
            throw new ApiException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
        }
        if (session.getFinAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_TERMINEE", "La session est terminée.");
        }
    }
}
