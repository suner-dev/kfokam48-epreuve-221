package com.kfokam48.presencerelecture.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kfokam48.presencerelecture.session.domain.SessionCours;
import com.kfokam48.presencerelecture.session.domain.SessionCoursRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Le seed de demonstration doit rester jouable sans desynchroniser le formateur : une session
 * encore ouverte voit sa fenetre de code rafraichie, une session terminee ou cloturee reste
 * terminee ou cloturee (ENF5, RG15, RG17, hypothese H1).
 */
@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {
    private static final Instant MAINTENANT = Instant.parse("2026-09-25T08:00:00Z");
    private static final Instant ANCIENNE_OUVERTURE = Instant.parse("2000-01-01T00:00:00Z");
    private static final Clock HORLOGE = Clock.fixed(MAINTENANT, ZoneOffset.UTC);

    @Mock
    private SessionCoursRepository repository;

    @Test
    void renouvelleLaFenetreDeLaSessionDemoEncoreOuverte() {
        SessionCours ouverte = sessionDemo();
        when(repository.findByCode("DEMO2026")).thenReturn(Optional.of(ouverte));

        new DemoDataInitializer(repository, HORLOGE).refreshDemoSession();

        assertThat(ouverte.getOuvertureAt()).isEqualTo(MAINTENANT);
        assertThat(ouverte.getExpirationAt()).isEqualTo(MAINTENANT.plusSeconds(15 * 60));
    }

    @Test
    void neTerminePlusUneSessionDemoDejaTerminee() {
        SessionCours terminee = sessionDemo();
        terminee.finish(MAINTENANT.minusSeconds(3600));
        when(repository.findByCode("DEMO2026")).thenReturn(Optional.of(terminee));

        new DemoDataInitializer(repository, HORLOGE).refreshDemoSession();

        assertThat(terminee.getFinAt()).isEqualTo(MAINTENANT.minusSeconds(3600));
        assertThat(terminee.getClotureAt()).isNull();
        assertThat(terminee.getOuvertureAt()).isEqualTo(ANCIENNE_OUVERTURE);
    }

    @Test
    void neRouvrePlusUneSessionDemoCloturee() {
        SessionCours cloturee = sessionDemo();
        cloturee.close(MAINTENANT.minusSeconds(3600), MAINTENANT.minusSeconds(1800));
        when(repository.findByCode("DEMO2026")).thenReturn(Optional.of(cloturee));

        new DemoDataInitializer(repository, HORLOGE).refreshDemoSession();

        assertThat(cloturee.getFinAt()).isEqualTo(MAINTENANT.minusSeconds(3600));
        assertThat(cloturee.getClotureAt()).isEqualTo(MAINTENANT.minusSeconds(1800));
    }

    @Test
    void neRendPlusMarquableLaSessionDemoApresRedemarrage() {
        SessionCours cloturee = sessionDemo();
        cloturee.close(MAINTENANT.minusSeconds(3600), MAINTENANT.minusSeconds(1800));
        when(repository.findByCode("DEMO2026")).thenReturn(Optional.of(cloturee));

        new DemoDataInitializer(repository, HORLOGE).refreshDemoSession();

        boolean encoreMarquable = cloturee.getClotureAt() == null && !cloturee.getExpirationAt().isBefore(MAINTENANT);
        assertThat(encoreMarquable)
                .as("une session cloturee ne doit pas redevenir marquable au redemarrage (RG15, H1)")
                .isFalse();
    }

    private static SessionCours sessionDemo() {
        return new SessionCours(
                "Session Démo",
                "DEMO2026",
                1L,
                ANCIENNE_OUVERTURE,
                ANCIENNE_OUVERTURE.plusSeconds(15 * 60)
        );
    }
}
