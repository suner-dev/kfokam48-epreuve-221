import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RequestError } from '../../core/models/api.models';

/**
 * Bandeau d'erreur ou d'information.
 *
 * Regle non negociable : quand l'API fournit un code et un message metier, ce
 * sont ces valeurs qui sont affichees, mot pour mot. Le frontend n'a jamais
 * l'autorite de resumer une regle de gestion qu'il ne connait pas, et un
 * message generic a la place du vrai code masquerait l'information utile.
 *
 * `toRequestError()` (core/services/api-error.ts) reste la seule source des
 * messages de repli.
 */
@Component({
  selector: 'ui-notice',
  template: `
    <div
      class="notice"
      [attr.data-tone]="teinte()"
      [attr.role]="roleEffectif()"
      [attr.aria-live]="roleEffectif() === 'alert' ? 'assertive' : 'polite'"
    >
      <span class="notice__icon" aria-hidden="true">
        @switch (teinte()) {
          @case ('success') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="m5 13 4 4L19 7" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          }
          @case ('danger') {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M12 8v5" stroke-linecap="round" />
              <circle cx="12" cy="16.5" r="0.6" fill="currentColor" />
              <circle cx="12" cy="12" r="9" />
            </svg>
          }
          @default {
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 11v5" stroke-linecap="round" />
              <circle cx="12" cy="7.8" r="0.6" fill="currentColor" />
            </svg>
          }
        }
      </span>

      <div class="notice__body">
        @if (erreur()?.code; as code) {
          <p class="notice__code kf-mono">{{ code }}</p>
        }
        <p class="notice__message">{{ erreur()?.message ?? message() }}</p>
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    .notice {
      display: flex;
      align-items: flex-start;
      gap: var(--kf-space-3);
      padding: var(--kf-space-4);
      border-radius: var(--kf-radius-md);
      border: 1px solid var(--kf-glass-border);
      background: var(--kf-glass-bg);
      backdrop-filter: blur(var(--kf-glass-blur)) saturate(var(--kf-glass-saturate));
      -webkit-backdrop-filter: blur(var(--kf-glass-blur)) saturate(var(--kf-glass-saturate));
      animation: kf-rise-in var(--kf-dur-3) var(--kf-ease-soft) both;
    }

    .notice__icon {
      display: grid;
      place-items: center;
      flex: none;
      width: 22px;
      height: 22px;
      margin-top: 1px;
    }

    .notice__icon svg {
      width: 100%;
      height: 100%;
    }

    .notice__body {
      min-width: 0;
    }

    .notice__code {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      color: var(--kf-text-muted);
      margin-bottom: 2px;
    }

    .notice__message {
      font-size: 0.92rem;
      line-height: 1.5;
      overflow-wrap: anywhere;
    }

    .notice[data-tone='danger'] {
      border-color: color-mix(in srgb, var(--kf-danger) 34%, transparent);
      background: var(--kf-danger-soft);
    }

    .notice[data-tone='danger'] .notice__icon,
    .notice[data-tone='danger'] .notice__message {
      color: var(--kf-danger);
    }

    .notice[data-tone='success'] {
      border-color: color-mix(in srgb, var(--kf-success) 34%, transparent);
      background: var(--kf-success-soft);
    }

    .notice[data-tone='success'] .notice__icon,
    .notice[data-tone='success'] .notice__message {
      color: var(--kf-success);
    }

    .notice[data-tone='info'] {
      border-color: color-mix(in srgb, var(--kf-info) 30%, transparent);
      background: var(--kf-info-soft);
    }

    .notice[data-tone='info'] .notice__icon,
    .notice[data-tone='info'] .notice__message {
      color: var(--kf-info);
    }

    /* Le texte du message garde la couleur de lecture normale : sur fond
       sombre, un rouge pur sur un aplat rose fatigue et perd le contraste.
       Seule l'icone porte la teinte d'etat. */
    .notice[data-tone='danger'] .notice__message {
      color: var(--kf-text);
    }

    .notice[data-tone='success'] .notice__message {
      color: var(--kf-text);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoticeComponent {
  /** Erreur issue de `toRequestError()` : `{code?, message, network}`. */
  readonly erreur = input<RequestError | null>(null);

  /** Message simple, pour un succes qui ne vient pas de l'API. */
  readonly message = input<string>('');

  /**
   * Force la teinte. Par defaut elle decoule de l'etat : rouge pour une erreur
   * metier, bleu pour une panne reseau. Seul un succes a besoin d etre impose,
   * parce que rien dans l'erreur ne dit qu'une action a reussi.
   */
  readonly tone = input<'danger' | 'success' | 'info' | null>(null);

  /**
   * `alert` interrompt le lecteur d'ecran : reserve a l'erreur. Un succes est
   * `status`, annonce sans interrompre ce que l'ecran est en train de lire.
   */
  readonly role = input<'alert' | 'status' | null>(null);

  protected readonly teinte = computed<'danger' | 'success' | 'info'>(() => {
    if (this.tone()) {
      return this.tone()!;
    }
    // Une panne reseau n'est pas une regle de gestion refusee : elle s'affiche
    // en information, pas en alarme rouge.
    return this.erreur()?.network ? 'info' : 'danger';
  });

  protected readonly roleEffectif = computed<'alert' | 'status'>(
    () => this.role() ?? (this.teinte() === 'danger' ? 'alert' : 'status'),
  );
}
