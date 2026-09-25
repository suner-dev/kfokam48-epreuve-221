import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { StatutExercice } from '../../core/models/api.models';

/**
 * Libelle et famille visuelle de chaque statut du cycle D4.
 *
 * Le statut est un choix de l'API : cette table ne fait que le decrire. Aucun
 * statut n'est invente ni deduit, et un statut inconnu reste affiche tel quel
 * plutot que masque.
 */
const STATUTS: Record<StatutExercice, { label: string; tone: Tone }> = {
  DEPOSE: { label: 'Déposé', tone: 'info' },
  EN_ATTENTE_DE_RELECTURE: { label: 'En attente de relecture', tone: 'warning' },
  EN_ATTENTE_SANS_RELECTEUR: { label: 'Sans relecteur', tone: 'danger' },
  RELU: { label: 'Relu', tone: 'success' },
  EN_ATTENTE_VERROUILLE: { label: 'En attente verrouillée', tone: 'warning' },
  RELU_VERROUILLE: { label: 'Relu verrouillé', tone: 'success' },
};

export type Tone = 'info' | 'warning' | 'danger' | 'success' | 'neutral';

/**
 * Pastille de statut.
 *
 * Le statut est doublement encode : par la couleur ET par le libelle texte.
 * La couleur seule exclutrait du sens les lecteurs d'ecran et les daltoniens ;
 * le libelle seul serait terne. Les deux ensemble, c'est lisible pour tout le
 * monde.
 */
@Component({
  selector: 'ui-status-pill',
  template: `
    <span class="pill" [attr.data-tone]="tone()">
      <span class="pill__dot" aria-hidden="true"></span>
      {{ texte() }}
    </span>
  `,
  styles: `
    .pill {
      display: inline-flex;
      align-items: center;
      gap: var(--kf-space-2);
      padding: 0.28rem 0.7rem 0.28rem 0.55rem;
      border-radius: var(--kf-radius-pill);
      border: 1px solid transparent;
      font-size: 0.78rem;
      font-weight: 600;
      line-height: 1.3;
      white-space: nowrap;
      transition:
        background var(--kf-dur-2) var(--kf-ease-soft),
        border-color var(--kf-dur-2) var(--kf-ease-soft);
    }

    .pill__dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
      box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 22%, transparent);
    }

    .pill[data-tone='info'] {
      color: var(--kf-info);
      background: var(--kf-info-soft);
      border-color: color-mix(in srgb, var(--kf-info) 34%, transparent);
    }

    .pill[data-tone='success'] {
      color: var(--kf-success);
      background: var(--kf-success-soft);
      border-color: color-mix(in srgb, var(--kf-success) 34%, transparent);
    }

    .pill[data-tone='warning'] {
      color: var(--kf-warning);
      background: var(--kf-warning-soft);
      border-color: color-mix(in srgb, var(--kf-warning) 34%, transparent);
    }

    .pill[data-tone='danger'] {
      color: var(--kf-danger);
      background: var(--kf-danger-soft);
      border-color: color-mix(in srgb, var(--kf-danger) 34%, transparent);
    }

    .pill[data-tone='neutral'] {
      color: var(--kf-text-soft);
      background: var(--kf-glass-bg);
      border-color: var(--kf-glass-border);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusPillComponent {
  /** Statut renvoye par l'API. */
  readonly statut = input.required<StatutExercice>();

  /** Libelle affiche. Par defaut, celui du statut ; surchargeable. */
  readonly label = input<string>();

  protected readonly tone = computed<Tone>(() => STATUTS[this.statut()]?.tone ?? 'neutral');

  /**
   * Un statut que cette table ne connait pas reste affiche sous sa forme
   * brute : masquer une donnee venue du serveur serait pire que de la montrer
   * sans mise en forme.
   */
  protected readonly texte = computed(() => this.label() ?? STATUTS[this.statut()]?.label ?? this.statut());
}
