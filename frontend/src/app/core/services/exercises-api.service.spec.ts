import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ExercisesApiService } from './exercises-api.service';

describe('ExercisesApiService', () => {
  let service: ExercisesApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ExercisesApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ExercisesApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('depose un exercice en POST', () => {
    service.submitExercise(3, 7, 'https://exemple.test/a').subscribe();

    const request = httpTesting.expectOne('/api/exercices');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ sessionId: 3, etudiantId: 7, lien: 'https://exemple.test/a' });
    request.flush({ id: 5, statut: 'EN_ATTENTE_DE_RELECTURE' });
  });

  it('liste les exercices d une session', () => {
    service.getSessionExercises(3).subscribe();

    const request = httpTesting.expectOne('/api/sessions/3/exercices');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  // EF9, Q13, RG12 — l'URL, la méthode et le corps exacts du PUT de remplacement.
  it('remplace un lien en PUT avec le seul champ lien', () => {
    service.replaceExerciseLink(6, 'https://exemple.test/b').subscribe();

    const request = httpTesting.expectOne('/api/exercices/6');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ lien: 'https://exemple.test/b' });
    request.flush({ id: 6, statut: 'EN_ATTENTE_DE_RELECTURE' });
  });
});
