import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TableauEtudiant } from '../models/api.models';
import { DashboardApiService } from './dashboard-api.service';

describe('DashboardApiService', () => {
  let service: DashboardApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardApiService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('transmet la moyenne retournée par l’API sans transformation', () => {
    const apiRows: TableauEtudiant[] = [
      {
        etudiantId: 12,
        nom: 'Ngansop Rainer',
        presences: 3,
        exercicesDeposes: 2,
        moyenne: 15.5,
        relecturesEnAttente: 1,
      },
    ];
    let received: TableauEtudiant[] | undefined;

    service.getDashboard(4).subscribe((rows) => (received = rows));

    const request = httpTesting.expectOne('/api/tableau?promotionId=4');
    expect(request.request.method).toBe('GET');
    request.flush(apiRows);

    expect(received).toEqual(apiRows);
    expect(received?.[0]?.moyenne).toBe(15.5);
  });
});
