import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReviewsApiService } from './reviews-api.service';

describe('ReviewsApiService', () => {
  let service: ReviewsApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReviewsApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReviewsApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  // Ce test affirmait l'absence de PUT. L'issue #64 exige au contraire qu'il évolue :
  // EF10 est autorisé, et c'est le POST qui reste le rendu initial.
  it('utilise le POST pour le rendu initial', () => {
    service.submitReview(17, { note: 18, commentaire: 'Analyse claire.' }).subscribe();

    const request = httpTesting.expectOne('/api/relectures/17');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ note: 18, commentaire: 'Analyse claire.' });
    request.flush(null);
  });

  // EF10, Q10, RG9 — l'URL, la méthode et le corps exacts du PUT de correction.
  it('utilise le PUT et le même corps pour corriger une relecture', () => {
    service.correctReview(17, { note: 14, commentaire: 'Version corrigee.' }).subscribe();

    const request = httpTesting.expectOne('/api/relectures/17');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ note: 14, commentaire: 'Version corrigee.' });
    request.flush(null);
  });
});
