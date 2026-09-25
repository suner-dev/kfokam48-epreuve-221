import { TestBed } from '@angular/core/testing';
import { SkeletonComponent } from './skeleton.component';

describe('SkeletonComponent', () => {
  async function create(label: string, height?: string) {
    await TestBed.configureTestingModule({ imports: [SkeletonComponent] }).compileComponents();

    const fixture = TestBed.createComponent(SkeletonComponent);
    fixture.componentRef.setInput('label', label);
    if (height !== undefined) {
      fixture.componentRef.setInput('height', height);
    }
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('n affiche aucune donnee fictive pendant le chargement', async () => {
    const fixture = await create('Chargement des promotions');
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    // Un squelette doit seulement suggérer la forme, jamais simuler un contenu. Un texte
    // factice (« 3 etudiants ») serait lu et cru par un lecteur d'ecran ou
    // annonce par la voix du navigateur.
    expect(texte.trim()).toBe('Chargement des promotions');
  });

  it('annonce ce qui charge, l ecran seul en connaissant le contexte', async () => {
    const fixture = await create('Chargement des promotions');
    const racine = (fixture.nativeElement as HTMLElement).querySelector('.skeleton');

    expect(racine?.getAttribute('role')).toBe('status');
    expect(racine?.getAttribute('aria-label')).toBe('Chargement des promotions');
  });

  it('reprend la hauteur demandee pour ne pas faire sauter la mise en page', async () => {
    const fixture = await create('Chargement', '3.5rem');
    const racine = (fixture.nativeElement as HTMLElement).querySelector('.skeleton') as HTMLElement;

    // Une hauteur stable evite que la page ne se reploie puis ne revienne
    // lorsque les donnees arrivent.
    expect(racine.style.height).toBe('3.5rem');
  });
});
