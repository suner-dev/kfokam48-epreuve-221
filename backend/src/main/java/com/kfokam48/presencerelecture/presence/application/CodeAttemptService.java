package com.kfokam48.presencerelecture.presence.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import com.kfokam48.presencerelecture.presence.domain.CodeTentative;
import com.kfokam48.presencerelecture.presence.domain.CodeTentativeRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CodeAttemptService {
    private static final int MAXIMUM_TENTATIVES = 5;
    private static final int DUREE_BLOCAGE_SECONDES = 120;

    private final CodeTentativeRepository repository;
    private final EtudiantRepository etudiantRepository;
    private final Clock clock;

    public CodeAttemptService(
            CodeTentativeRepository repository,
            EtudiantRepository etudiantRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.etudiantRepository = etudiantRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void checkBlocked(Long etudiantId) {
        CodeTentative tentative = repository.findByEtudiantId(etudiantId).orElse(null);
        if (tentative != null
                && tentative.getBloqueJusqua() != null
                && tentative.getBloqueJusqua().isAfter(clock.instant())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TROP_ESSAIS", "Trop de tentatives invalides. Réessayez plus tard.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(Long etudiantId) {
        if (!etudiantRepository.existsById(etudiantId)) {
            return;
        }
        CodeTentative tentative = repository.findByEtudiantId(etudiantId)
                .orElseGet(() -> new CodeTentative(etudiantId));
        Instant maintenant = clock.instant();
        if (tentative.getBloqueJusqua() != null && !tentative.getBloqueJusqua().isAfter(maintenant)) {
            tentative.reset();
        }
        tentative.registerFailure(maintenant, MAXIMUM_TENTATIVES, DUREE_BLOCAGE_SECONDES);
        repository.save(tentative);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerSuccess(Long etudiantId) {
        repository.findByEtudiantId(etudiantId).ifPresent(tentative -> {
            tentative.reset();
            repository.save(tentative);
        });
    }
}
