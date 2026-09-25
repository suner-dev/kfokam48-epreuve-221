import { provideHttpClient } from '@angular/common/http';
import { provideLocationMocks } from '@angular/common/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { routes } from './app.routes';
import { ReferencesApiService } from './core/services/references-api.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideLocationMocks(),
        provideRouter(routes),
        {
          provide: ReferencesApiService,
          useValue: { getPromotions: () => of([]), getEtudiants: () => of([]) },
        },
      ],
    }).compileComponents();
  });

  it('affiche la navigation entre les trois parcours', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Formateur');
    expect(text).toContain('Étudiant');
    expect(text).toContain('Relecteur');
  });

  it('marque le parcours courant comme actif', async () => {
    const router = TestBed.inject<Router>(Router);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    // Apres navigation, exactement un parcours est actif et l'indicateur
    // mesure ce lien-la. Sur une route inconnue, aucun ne le serait.
    await router.navigateByUrl('/etudiant');
    fixture.detectChanges();

    const active = (fixture.nativeElement as HTMLElement).querySelectorAll(
      '.kf-pillnav__link.is-active',
    );

    // Une seule route active a la fois : l'indicateur ne peut pas hesiter.
    expect(active).toHaveLength(1);
    expect(active[0].textContent).toContain('Étudiant');
  });

  it('expose un fond decoratif et un pied de page sans dupliquer le contenu', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    // Le fond aurora est decoratif : il doit rester hors du flux d'accessibilite.
    expect(element.querySelector('.kf-aurora')?.getAttribute('aria-hidden')).toBe('true');
    expect(element.querySelector('main.app-main')).not.toBeNull();
    expect(element.querySelector('footer')).not.toBeNull();
  });
});
