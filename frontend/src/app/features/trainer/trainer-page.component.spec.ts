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

  it('distingue visuellement les six statuts du cycle D4', { timeout: 30000 }, async () => {
    const fixture = await createComponent(() => of([]));
    const component = fixture.componentInstance;
    component.currentSession.set({
      id: 9,
      titre: 'Session D4',
      code: 'ABCDEFGH',
      promotionId: 4,
      ouvertureAt: '2026-09-25T08:00:00Z',
      expirationAt: '2026-09-25T08:15:00Z',
      finAt: null,
      clotureAt: null,
    });
    component.exercises.set([
      { id: 1, etudiantId: 1, statut: 'DEPOSE', relecteurId: null, commenceeAt: null, rendueAt: null },
      { id: 2, etudiantId: 2, statut: 'EN_ATTENTE_DE_RELECTURE', relecteurId: 3, commenceeAt: null, rendueAt: null },
      { id: 3, etudiantId: 3, statut: 'EN_ATTENTE_SANS_RELECTEUR', relecteurId: null, commenceeAt: null, rendueAt: null },
      { id: 4, etudiantId: 4, statut: 'RELU', relecteurId: 5, commenceeAt: '2026-09-25T08:01:00Z', rendueAt: '2026-09-25T08:02:00Z' },
      { id: 5, etudiantId: 5, statut: 'EN_ATTENTE_VERROUILLE', relecteurId: 6, commenceeAt: null, rendueAt: null },
      { id: 6, etudiantId: 6, statut: 'RELU_VERROUILLE', relecteurId: 7, commenceeAt: '2026-09-25T08:01:00Z', rendueAt: '2026-09-25T08:02:00Z' },
    ]);
    component.exercisesStatus.set('success');

    fixture.detectChanges();

    const badges = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('[data-status]'),
    );
    expect(badges.map((badge) => badge.dataset['status'])).toEqual([
      'DEPOSE',
      'EN_ATTENTE_DE_RELECTURE',
      'EN_ATTENTE_SANS_RELECTEUR',
      'RELU',
      'EN_ATTENTE_VERROUILLE',
      'RELU_VERROUILLE',
    ]);
    expect(badges.map((badge) => badge.className)).toEqual([
      'status-badge status-depose',
      'status-badge status-attente-relecture',
      'status-badge status-attente-sans-relecteur',
      'status-badge status-relu',
      'status-badge status-attente-verrouillee',
      'status-badge status-relu-verrouillee',
    ]);
  });

  it('affiche les promotions après un chargement réussi', async () => {
    const fixture = await createComponent(() => of([{ id: 4, nom: 'KFOKAM48-2026' }]));

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('KFOKAM48-2026');
  });
});
