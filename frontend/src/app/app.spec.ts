import { provideHttpClient } from '@angular/common/http';
import { provideLocationMocks } from '@angular/common/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
});
