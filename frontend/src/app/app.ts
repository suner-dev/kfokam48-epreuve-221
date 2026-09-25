import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

/**
 * Coquille applicative : en-tete vitreux, navigation entre les trois parcours,
 * fond « aurora » et pied de page.
 *
 * Elle n'encode aucune regle metier : elle se contente d'habiliter les ecrans
 * et de mesurer, pour la seule mise en forme, la position du lien actif.
 */
@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly nav = viewChild<ElementRef<HTMLElement>>('nav');

  /** Position et largeur du lien actif, publiees en variables CSS. */
  private readonly indicator = signal({ x: 0, width: 0 });

  /** Variables CSS consommees par `.kf-pillnav__indicator` dans styles.css. */
  protected readonly indicatorStyle = computed(() => ({
    '--nav-x': `${this.indicator().x}px`,
    '--nav-larg': `${this.indicator().width}px`,
  }));

  constructor() {
    // afterNextRender : le lien actif n'existe qu'une fois le DOM construit.
    afterNextRender(() => {
      this.measureIndicator();
      this.router.events
        .pipe(
          filter((event) => event instanceof NavigationEnd),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe(() => this.measureIndicator());
    });
  }

  /**
   * Mesure le lien marque actif et publie sa position.
   *
   * Le selecteur cible la classe de liaison partagee `.kf-pillnav__link` : la
   * mesure reste ainsi exacte meme si l'ecran ajoute ses propres classes.
   *
   * En l'absence de lien actif (route inconnue) l'indicateur reste a zero :
   * son opacite 0 le masque, et la navigation demeure utilisable.
   */
  private measureIndicator(): void {
    const nav = this.nav()?.nativeElement;
    const active = nav?.querySelector<HTMLElement>('.kf-pillnav__link.is-active');

    this.indicator.set(
      active ? { x: active.offsetLeft, width: active.offsetWidth } : { x: 0, width: 0 },
    );
  }
}
