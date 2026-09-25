import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { routes } from './app.routes';
import { ReferencesApiService } from './core/services/references-api.service';

describe('application routes', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<App>>;
  let router: Router;

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

    fixture = TestBed.createComponent(App);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it.each([
    ['/formateur', 'Gérer une session et suivre la promotion'],
    ['/etudiant', 'Parcours étudiant'],
    ['/relecteur', 'Parcours relecteur'],
  ])('charge la route %s', async (url, heading) => {
    await router.navigateByUrl(url);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(heading);
  });
});
