import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';

/**
 * Bouton du design system.
 *
 * Il ne fait qu'afficher une intention : la validation, l'etat de chargement et
 * les regles metier restent la responsabilite de l'ecran et de l'API. `disabled`
 * est expose tel quel pour qu'un ecran puisse interdire une action que le
 * serveur refuserait de toute facon.
 */
@Component({
  selector: 'ui-button',
  template: `
    <button
      class="btn"
      type="button"
      [attr.data-variant]="variant()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading() || null"
    >
      @if (loading()) {
        <span class="btn__spinner" aria-hidden="true"></span>
      }
      <ng-content />
    </button>
  `,
  styles: `
    .btn {
      position: relative;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--kf-space-2);
      padding: 0.68rem 1.35rem;
      border: 1px solid transparent;
      border-radius: var(--kf-radius-pill);
      font-family: var(--kf-font-body);
      font-size: 0.92rem;
      font-weight: 600;
      line-height: 1.2;
      cursor: pointer;
      overflow: hidden;
      transition:
        transform var(--kf-dur-2) var(--kf-ease-spring),
        box-shadow var(--kf-dur-2) var(--kf-ease-soft),
        background var(--kf-dur-2) var(--kf-ease-soft),
        border-color var(--kf-dur-2) var(--kf-ease-soft),
        opacity var(--kf-dur-2) var(--kf-ease-soft);
    }

    .btn:hover:not(:disabled) {
      transform: translateY(-2px);
    }

    .btn:active:not(:disabled) {
      transform: translateY(0) scale(0.985);
      transition-duration: var(--kf-dur-1);
    }

    .btn:disabled {
      cursor: not-allowed;
      opacity: 0.45;
      transform: none;
    }

    /* Balayage lumineux au survol : purement decoratif, et contenu dans
       "overflow: hidden" pour ne pas deborder du bouton. */
    .btn::after {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(
        100deg,
        transparent 20%,
        rgba(255, 255, 255, 0.22) 50%,
        transparent 80%
      );
      transform: translateX(-120%);
      transition: transform var(--kf-dur-4) var(--kf-ease-soft);
      pointer-events: none;
    }

    .btn:hover:not(:disabled)::after {
      transform: translateX(120%);
    }

    .btn[data-variant='primary'] {
      color: #fff;
      background: linear-gradient(120deg, var(--kf-indigo-500), var(--kf-violet-500));
      box-shadow: 0 10px 26px rgba(99, 102, 241, 0.4);
    }

    .btn[data-variant='primary']:hover:not(:disabled) {
      box-shadow: 0 16px 38px rgba(99, 102, 241, 0.55);
    }

    .btn[data-variant='secondary'] {
      color: var(--kf-text-strong);
      background: var(--kf-glass-bg-strong);
      border-color: var(--kf-glass-border-strong);
      backdrop-filter: blur(var(--kf-glass-blur)) saturate(var(--kf-glass-saturate));
      -webkit-backdrop-filter: blur(var(--kf-glass-blur)) saturate(var(--kf-glass-saturate));
    }

    .btn[data-variant='secondary']:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.14);
      border-color: var(--kf-glass-border-strong);
    }

    .btn[data-variant='danger'] {
      color: #fff;
      background: linear-gradient(120deg, #f43f5e, var(--kf-danger));
      box-shadow: 0 10px 26px rgba(244, 63, 94, 0.35);
    }

    .btn[data-variant='ghost'] {
      color: var(--kf-text-soft);
      background: transparent;
    }

    .btn[data-variant='ghost']:hover:not(:disabled) {
      color: var(--kf-text-strong);
      background: var(--kf-glass-bg);
    }

    .btn__spinner {
      width: 14px;
      height: 14px;
      border: 2px solid rgba(255, 255, 255, 0.35);
      border-top-color: #fff;
      border-radius: 50%;
      animation: btn-spin 720ms linear infinite;
    }

    @keyframes btn-spin {
      to {
        transform: rotate(360deg);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly disabled = input(false);

  /** Affiche un indicateur d'activite et neutralise le bouton. */
  readonly loading = input(false);
}
