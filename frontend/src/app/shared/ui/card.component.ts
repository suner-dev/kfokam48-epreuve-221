import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Carte de verre, conteneur de mise en page par defaut des ecrans.
 *
 * Elle apporte la surface, l'ombre et l'entree en douceur, rien d'autre :
 * aucun contenu, aucune regle. Le halo suit le curseur via `--kf-x`/`--kf-y`,
 * que l'ecran met a jour ; en l'absence de ces variables le halo reste centre
 * et immobile, ce qui evite tout scintillement si le survol n'est pas gere.
 */
@Component({
  selector: 'ui-card',
  template: `
    <div class="card" [attr.data-tone]="tone()">
      <div class="card__halo" aria-hidden="true"></div>
      <div class="card__body">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    .card {
      position: relative;
      border-radius: var(--kf-radius-lg);
      border: 1px solid var(--kf-glass-border);
      background: var(--kf-glass-bg);
      backdrop-filter: blur(var(--kf-glass-blur)) saturate(var(--kf-glass-saturate));
      -webkit-backdrop-filter: blur(var(--kf-glass-blur)) saturate(var(--kf-glass-saturate));
      box-shadow: var(--kf-shadow-md);
      overflow: hidden;
      transition:
        transform var(--kf-dur-3) var(--kf-ease-soft),
        border-color var(--kf-dur-3) var(--kf-ease-soft),
        box-shadow var(--kf-dur-3) var(--kf-ease-soft);
    }

    .card:hover {
      transform: translateY(-3px);
      border-color: var(--kf-glass-border-strong);
      box-shadow: var(--kf-shadow-lg);
    }

    .card__body {
      position: relative;
      z-index: 1;
      padding: var(--kf-space-5);
    }

    .card[data-tone='accent'] {
      border-color: color-mix(in srgb, var(--kf-accent) 30%, transparent);
      background: linear-gradient(
        150deg,
        color-mix(in srgb, var(--kf-accent) 12%, transparent),
        var(--kf-glass-bg) 60%
      );
    }

    .card__halo {
      position: absolute;
      inset: -1px;
      z-index: 0;
      border-radius: inherit;
      background: radial-gradient(
        340px circle at var(--kf-x, 50%) var(--kf-y, 0%),
        rgba(255, 255, 255, 0.14),
        transparent 62%
      );
      opacity: 0;
      transition: opacity var(--kf-dur-3) var(--kf-ease-soft);
      pointer-events: none;
    }

    .card:hover .card__halo {
      opacity: 1;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  /** `accent` reserve la carte a un usage particulier (etat, compte cle). */
  readonly tone = input<'default' | 'accent'>('default');
}
