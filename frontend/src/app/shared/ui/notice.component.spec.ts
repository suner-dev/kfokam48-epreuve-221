import { TestBed } from '@angular/core/testing';
import { NoticeComponent } from './notice.component';

describe('NoticeComponent', () => {
  async function create(erreur: unknown, extra?: { message?: string; tone?: string; role?: string }) {
    await TestBed.configureTestingModule({ imports: [NoticeComponent] }).compileComponents();

    const fixture = TestBed.createComponent(NoticeComponent);
    fixture.componentRef.setInput('erreur', erreur);
    for (const [cle, valeur] of Object.entries(extra ?? {})) {
      fixture.componentRef.setInput(cle, valeur);
    }
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('affiche le code et le message metier fournis par l API, mot pour mot', async () => {
    const fixture = await create({
      code: 'SESSION_CLOTUREE',
      message: 'La session est clôturée, plus aucune présence ne peut être marquée.',
    });
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('SESSION_CLOTUREE');
    expect(texte).toContain('La session est clôturée, plus aucune présence ne peut être marquée.');
  });

  it('n invite jamais la regle metier a un message que l API n a pas donne', async () => {
    // L'API est seule autorisee a decrire la regle. Si le frontend retapissait
    // «SESSION_CLOTUREE» en « Cette session n'existe plus », il inventerait une
    // traduction de la regle et perdrait le code recherche dans les journaux.
    const fixture = await create({ code: 'DEJA_PRESENT', message: 'Présence déjà marquée.' });
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('DEJA_PRESENT');
    expect(texte).not.toContain('n’existe plus');
  });

  it('interrompt le lecteur d ecran pour une erreur metier', async () => {
    const fixture = await create({ code: 'CHAMP_MANQUANT', message: 'Le code est obligatoire.' });
    const racine = (fixture.nativeElement as HTMLElement).querySelector('.notice');

    expect(racine?.getAttribute('role')).toBe('alert');
    expect(racine?.getAttribute('aria-live')).toBe('assertive');
  });

  it('n interrompt pas le lecteur d ecran pour un succes', async () => {
    const fixture = await create(null, { message: 'Présence enregistrée.', tone: 'success' });
    const racine = (fixture.nativeElement as HTMLElement).querySelector('.notice');

    // Un succes ne doit pas couper la lecture en cours pour une information
    // que l'utilisateur n'attendait pas.
    expect(racine?.getAttribute('role')).toBe('status');
    expect(racine?.getAttribute('aria-live')).toBe('polite');
  });

  it('traite une panne reseau comme une information, pas comme une alarme', async () => {
    const fixture = await create({ message: 'Serveur injoignable.', network: true });
    const racine = (fixture.nativeElement as HTMLElement).querySelector('.notice');

    // Une panne n'est pas une regle de gestion refusee : l'annoncer en rouge
    // `alert` ferait croire a un rejet metier.
    expect(racine?.getAttribute('data-tone')).toBe('info');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Serveur injoignable.');
  });

  it('affiche le message simple quand l API n a rien fourni', async () => {
    const fixture = await create(null, { message: 'Copiez le code reçu.' });

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Copiez le code reçu.');
  });
});
