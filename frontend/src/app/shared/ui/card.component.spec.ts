import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CardComponent } from './card.component';

@Component({
  imports: [CardComponent],
  template: `<ui-card [tone]="tone()"><p class="contenu">Contenu</p></ui-card>`,
})
class HoteComponent {
  readonly tone = signal<'default' | 'accent'>('default');
}

describe('CardComponent', () => {
  async function create() {
    await TestBed.configureTestingModule({ imports: [HoteComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HoteComponent);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('projette le contenu fourni par l ecran', async () => {
    const fixture = await create();

    // Le conteneur ne doit rien decided du contenu : c'est l'ecran qui sait
    // ce qu'il place dans sa carte.
    expect((fixture.nativeElement as HTMLElement).querySelector('.contenu')?.textContent).toBe('Contenu');
  });

  it('marque son halo comme decoratif', async () => {
    const fixture = await create();
    const halo = (fixture.nativeElement as HTMLElement).querySelector('.card__halo');

    // Le halo suit le curseur : il n'apporte aucune information et ne doit
    // donc pas etre annonce par un lecteur d'ecran.
    expect(halo?.getAttribute('aria-hidden')).toBe('true');
  });

  it('expose la variante de surface pour que l accent reste explicite', async () => {
    const fixture = await create();
    const carte = (fixture.nativeElement as HTMLElement).querySelector('.card');

    expect(carte?.getAttribute('data-tone')).toBe('default');

    fixture.componentInstance.tone.set('accent');
    fixture.detectChanges();

    // L'accent distingue visuellement une carte d'etat ou de compte cle ; sans
    // attribut expose, la variante resterait invisible et inverifiable.
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.card')?.getAttribute('data-tone'),
    ).toBe('accent');
  });
});
