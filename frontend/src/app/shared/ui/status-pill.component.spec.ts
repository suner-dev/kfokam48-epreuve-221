import { TestBed } from '@angular/core/testing';
import { StatusPillComponent } from './status-pill.component';

describe('StatusPillComponent', () => {
  async function create(statut: string, label?: string) {
    await TestBed.configureTestingModule({ imports: [StatusPillComponent] }).compileComponents();

    const fixture = TestBed.createComponent(StatusPillComponent);
    fixture.componentRef.setInput('statut', statut);
    if (label !== undefined) {
      fixture.componentRef.setInput('label', label);
    }
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it.each([
    ['DEPOSE', 'Déposé'],
    ['EN_ATTENTE_DE_RELECTURE', 'En attente de relecture'],
    ['EN_ATTENTE_SANS_RELECTEUR', 'Sans relecteur'],
    ['RELU', 'Relu'],
    ['EN_ATTENTE_VERROUILLE', 'En attente verrouillée'],
    ['RELU_VERROUILLE', 'Relu verrouillé'],
  ])('nomme le statut %s en toutes lettres', async (statut, attendu) => {
    const fixture = await create(statut);

    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain(attendu);
  });

  it('ne repose jamais sur la seule couleur pour porter le sens', async () => {
    const fixture = await create('EN_ATTENTE_SANS_RELECTEUR');
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    // La pastille porte un libelle lisible : sans lui, un daltonien ou un
    // lecteur d'ecran ne distinguerait pas « Sans relecteur » d'un succes.
    expect(texte.trim().length).toBeGreaterThan(0);
    // La pastille decororative est masquee, elle ne doit pas etre annoncee.
    expect((fixture.nativeElement as HTMLElement).querySelector('.pill__dot')?.getAttribute('aria-hidden')).toBe(
      'true',
    );
  });

  it('affiche un statut inconnu tel quel plutot que de le masquer', async () => {
    const fixture = await create('STATUT_INVENTE');

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('STATUT_INVENTE');
  });

  it('laisse un libelle surchargeable prendre le pas', async () => {
    const fixture = await create('RELU', 'Relecture terminée');

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Relecture terminée');
  });
});
