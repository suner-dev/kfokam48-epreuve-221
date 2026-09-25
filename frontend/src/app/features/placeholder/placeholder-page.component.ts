import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-placeholder-page',
  imports: [RouterLink],
  template: `
    <section class="placeholder" aria-labelledby="placeholder-title">
      <h1 id="placeholder-title">{{ title }}</h1>
      <p>{{ description }}</p>
      <a routerLink="/formateur">Accéder au parcours formateur</a>
    </section>
  `,
  styles: `
    .placeholder { max-width: 46rem; margin: 3rem auto; padding: 2rem; background: #fff; border-radius: .75rem; box-shadow: 0 1px 3px #0002; }
    a { color: #114e93; font-weight: 700; }
  `,
})
export class PlaceholderPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly routeData = this.route.snapshot.data;

  protected readonly title = typeof this.routeData['title'] === 'string' ? this.routeData['title'] : 'Parcours en préparation';
  protected readonly description =
    typeof this.routeData['description'] === 'string'
      ? this.routeData['description']
      : 'Cette fonctionnalité sera disponible prochainement.';
}
