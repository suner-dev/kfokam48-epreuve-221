package com.kfokam48.presencerelecture.presence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import com.kfokam48.presencerelecture.presence.domain.CodeTentative;
import com.kfokam48.presencerelecture.presence.domain.CodeTentativeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CodeAttemptServiceTest {
    private static final Instant MAINTENANT = Instant.parse("2026-09-25T08:00:00Z");
    private static final long ETUDIANT_ID = 12L;

    @Mock
    private CodeTentativeRepository repository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private Clock clock;

    @Test
    void bloqueALaCinqiemeSaisieInvalide() {
        when(clock.instant()).thenReturn(MAINTENANT);
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        CodeTentative tentative = new CodeTentative(ETUDIANT_ID);
        when(repository.findByEtudiantId(ETUDIANT_ID)).thenReturn(Optional.of(tentative));
        CodeAttemptService service = new CodeAttemptService(repository, etudiantRepository, clock);

        for (int index = 0; index < 5; index++) {
            service.registerFailure(ETUDIANT_ID);
        }

        assertThat(tentative.getTentatives()).isEqualTo(5);
        assertThat(tentative.getBloqueJusqua()).isEqualTo(MAINTENANT.plusSeconds(120));
        verify(repository, times(5)).save(tentative);
    }

    @Test
    void refuseUneNouvelleSaisiePendantLeBlocage() {
        CodeTentative tentative = new CodeTentative(ETUDIANT_ID);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        when(repository.findByEtudiantId(ETUDIANT_ID)).thenReturn(Optional.of(tentative));
        when(clock.instant()).thenReturn(MAINTENANT.plusSeconds(60));
        CodeAttemptService service = new CodeAttemptService(repository, etudiantRepository, clock);

        assertThatThrownBy(() -> service.checkBlocked(ETUDIANT_ID))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.code()).isEqualTo("TROP_ESSAIS");
                });
    }

    @Test
    void leveLeBlocageApresExpirationEtRemetAZeroSurSucces() {
        CodeTentative tentative = new CodeTentative(ETUDIANT_ID);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        tentative.registerFailure(MAINTENANT, 5, 120);
        when(repository.findByEtudiantId(ETUDIANT_ID)).thenReturn(Optional.of(tentative));
        when(clock.instant()).thenReturn(MAINTENANT.plusSeconds(121));
        CodeAttemptService service = new CodeAttemptService(repository, etudiantRepository, clock);

        service.checkBlocked(ETUDIANT_ID);
        service.registerSuccess(ETUDIANT_ID);

        assertThat(tentative.getTentatives()).isZero();
        assertThat(tentative.getBloqueJusqua()).isNull();
    }
}
