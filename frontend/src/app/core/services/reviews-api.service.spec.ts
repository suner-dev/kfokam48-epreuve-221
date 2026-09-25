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

  it('utilise le POST initial et n’expose pas de correction PUT dans v0.1', () => {
    expect('updateReview' in (service as object)).toBe(false);

    service.submitReview(17, { note: 18, commentaire: 'Analyse claire.' }).subscribe();

    const request = httpTesting.expectOne('/api/relectures/17');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ note: 18, commentaire: 'Analyse claire.' });
    request.flush(null);
  });
});
