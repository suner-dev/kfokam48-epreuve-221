import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Squelette de chargement.
 *
 * Il annonce une forme, jamais une donnee : aucun texte fictif n'est rendu,
 * donc rien ne peut etre lu par erreur comme un contenu reel. Le `role="status"`
 * et le libelle viennent de l'ecran, qui seul connait le contexte
 * (« Chargement des promotions… ») et doit donc le passer explicitement.
 */
@Component({
  selector: 'ui-skeleton',
  template: `
    <div class="skeleton" [style.height]="height()" role="status" [attr.aria-label]="label()">
      <span class="kf-sr-only">{{ label() }}</span>
    </div>
  `,
  styles: `
    .skeleton {
      width: 100%;
      border-radius: var(--kf-radius-sm);
      background: linear-gradient(
        100deg,
        var(--kf-glass-bg) 20%,
        var(--kf-glass-bg-strong) 42%,
        var(--kf-glass-bg) 64%
      );
      background-size: 220% 100%;
      animation:
        kf-pulse var(--kf-dur-3) var(--kf-ease-inout) infinite alternate,
        kf-shimmer 1.5s var(--kf-ease-inout) infinite;
    }

    @keyframes kf-shimmer {
      from {
        background-position: 140% 0;
      }
      to {
        background-position: -40% 0;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonComponent {
  /** Hauteur, pour que le squelette reproduise l'emplacement du contenu. */
  readonly height = input('1rem');

  /** Description de ce qui charge, annoncee aux lecteurs d'ecran. */
  readonly label = input.required<string>();
}
