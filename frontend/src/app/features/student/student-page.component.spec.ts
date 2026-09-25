import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { Promotion } from '../../core/models/api.models';
import { ExercisesApiService } from '../../core/services/exercises-api.service';
import { PresencesApiService } from '../../core/services/presences-api.service';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { ReviewsApiService } from '../../core/services/reviews-api.service';
import { StudentPageComponent } from './student-page.component';

async function createComponent(getPromotions: () => Observable<Promotion[]>) {
  await TestBed.configureTestingModule({
    imports: [StudentPageComponent],
    providers: [
      { provide: ReferencesApiService, useValue: { getPromotions, getEtudiants: () => of([]) } },
      { provide: PresencesApiService, useValue: {} },
      { provide: ExercisesApiService, useValue: {} },
      { provide: ReviewsApiService, useValue: {} },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(StudentPageComponent);
  fixture.detectChanges();
  return fixture;
}

describe('StudentPageComponent', () => {
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
            status: 404,
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
