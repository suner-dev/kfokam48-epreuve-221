import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PresencesApiService } from './presences-api.service';

describe('PresencesApiService', () => {
  let service: PresencesApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PresencesApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PresencesApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('envoie le chemin et le corps imposés pour marquer une présence', () => {
    service.markPresence('KF48-2026', 28).subscribe();

    const request = httpTesting.expectOne('/api/presences');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ code: 'KF48-2026', etudiantId: 28 });
    request.flush({ id: 1, sessionId: 2, etudiantId: 28, source: 'ETUDIANT' });
  });
});
