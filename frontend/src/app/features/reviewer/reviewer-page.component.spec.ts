import { HttpErrorResponse } from '@angular/common/http';
import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { Promotion } from '../../core/models/api.models';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { ReviewsApiService } from '../../core/services/reviews-api.service';
import { ReviewerPageComponent } from './reviewer-page.component';

async function createComponent(
  getPromotions: () => Observable<Promotion[]>,
  submitReview: (body: { note: number; commentaire: string }) => Observable<unknown> = () => of(undefined),
) {
  await TestBed.configureTestingModule({
    imports: [ReviewerPageComponent],
    providers: [
      { provide: ReferencesApiService, useValue: { getPromotions, getEtudiants: () => of([]) } },
      { provide: ReviewsApiService, useValue: { submitReview } },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(ReviewerPageComponent);
  fixture.detectChanges();
  return fixture;
}

describe('ReviewerPageComponent', () => {
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

  it('refuse une note décimale avant l’appel API', async () => {
    const submitReview = vi.fn(() => of(undefined));
    const fixture = await createComponent(() => of([]), submitReview);
    const component = fixture.componentInstance;
    component.activeReview.set({
      relectureId: 7,
      exerciceId: 8,
      lienExercice: 'https://example.test/exercice',
      commenceeAt: null,
    });
    component.reviewForm.setValue({ note: 12.5, commentaire: 'Commentaire valide.' });

    component.submitReview();

    expect(component.reviewForm.controls.note.hasError('integer')).toBe(true);
    expect(submitReview).not.toHaveBeenCalled();
  });

  it('affiche les promotions après un chargement réussi', async () => {
    const fixture = await createComponent(() => of([{ id: 4, nom: 'KFOKAM48-2026' }]));

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('KFOKAM48-2026');
  });
});
