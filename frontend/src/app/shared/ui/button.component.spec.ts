import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ButtonComponent } from './button.component';

@Component({
  imports: [ButtonComponent],
  template: `<ui-button [variant]="variante()" [loading]="chargement()" />`,
})
class HoteComponent {
  readonly variante = signal<'primary' | 'secondary' | 'danger' | 'ghost'>('primary');
  readonly chargement = signal(false);
}

describe('ButtonComponent', () => {
  async function create() {
    await TestBed.configureTestingModule({ imports: [HoteComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HoteComponent);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('rend un vrai bouton, donc atteignable au clavier', async () => {
    const fixture = await create();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('button');

    // Un <div> cliquable serait invisible pour la tabulation : l'action
    // deviendrait impossible au clavier uniquement.
    expect(bouton).not.toBeNull();
    expect(bouton?.tagName).toBe('BUTTON');
    expect(bouton?.getAttribute('type')).toBe('button');
  });

  it('neutralise le bouton pendant un envoi pour eviter un double depot', async () => {
    const fixture = await create();
    fixture.componentInstance.chargement.set(true);
    fixture.detectChanges();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('button');

    // Sans ce verrou, un second clic renverrait un second POST et l'API
    // reponderait 409 : l'utilisateur verrait sa propre action lui echapper.
    expect(bouton?.disabled).toBe(true);
    expect(bouton?.getAttribute('aria-busy')).toBe('true');
  });

  it('expose la variante demandee pour que la couleur reste un choix explicite', async () => {
    const fixture = await create();
    fixture.componentInstance.variante.set('danger');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('button')?.getAttribute('data-variant')).toBe(
      'danger',
    );
  });
});
