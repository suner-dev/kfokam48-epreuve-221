import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { Promotion } from '../../core/models/api.models';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { ExercisesApiService } from '../../core/services/exercises-api.service';
import { PresencesApiService } from '../../core/services/presences-api.service';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { SessionsApiService } from '../../core/services/sessions-api.service';
import { TrainerPageComponent } from './trainer-page.component';

async function createComponent(getPromotions: () => Observable<Promotion[]>) {
  await TestBed.configureTestingModule({
    imports: [TrainerPageComponent],
    providers: [
      { provide: ReferencesApiService, useValue: { getPromotions, getEtudiants: () => of([]) } },
      { provide: SessionsApiService, useValue: {} },
      { provide: PresencesApiService, useValue: {} },
      { provide: ExercisesApiService, useValue: {} },
      { provide: DashboardApiService, useValue: {} },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(TrainerPageComponent);
  fixture.detectChanges();
  return fixture;
}

describe('TrainerPageComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('affiche le chargement des promotions', async () => {
    const promotions = new Subject<Promotion[]>();
    const fixture = await createComponent(() => promotions);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Chargement des promotions');
  });

  it('affiche une erreur métier fournie par l’API', async () => {
    const fixture = await createComponent(() =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 410,
            error: { code: 'PROMOTION_INCONNUE', message: 'La promotion demandée est inconnue.' },
          }),
      ),
    );

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('PROMOTION_INCONNUE');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('La promotion demandée est inconnue.');
  });

  it('affiche les promotions après un chargement réussi', async () => {
    const fixture = await createComponent(() => of([{ id: 4, nom: 'KFOKAM48-2026' }]));

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('KFOKAM48-2026');
  });
});
