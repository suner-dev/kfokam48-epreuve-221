import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { Promotion, RelectureRecue } from '../../core/models/api.models';
import { ExercisesApiService } from '../../core/services/exercises-api.service';
import { PresencesApiService } from '../../core/services/presences-api.service';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { ReviewsApiService } from '../../core/services/reviews-api.service';
import { StudentPageComponent } from './student-page.component';

function review(overrides: Partial<RelectureRecue>): RelectureRecue {
  return {
    exerciceId: 1,
    sessionId: 1,
    lienExercice: 'https://exemple.test/exercice/1',
    note: null,
    commentaire: '',
    nbNotes: 0,
    provisoire: true,
    ...overrides,
  };
}

async function createComponent(
  getPromotions: () => Observable<Promotion[]>,
  getRecues: () => Observable<RelectureRecue[]> = () => of([]),
  etudiantId = 0,
) {
  await TestBed.configureTestingModule({
    imports: [StudentPageComponent],
    providers: [
      { provide: ReferencesApiService, useValue: { getPromotions, getEtudiants: () => of([]) } },
      { provide: PresencesApiService, useValue: {} },
      { provide: ExercisesApiService, useValue: {} },
      { provide: ReviewsApiService, useValue: { getReceivedReviews: getRecues } },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(StudentPageComponent);
  fixture.detectChanges();
  if (etudiantId > 0) {
    // Choisir son nom est le geste qui declenche le chargement des resultats recus.
    fixture.componentInstance.identityForm.controls.etudiantId.setValue(etudiantId);
    fixture.componentInstance.onStudentChanged();
    fixture.detectChanges();
  }
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

  // Enveloppe étape 3 : la note affichée vient de l'API, le frontend ne la recalcule pas (F3).
  it('marque la note comme provisoire tant qu’un seul des deux pairs a rendu', async () => {
    const fixture = await createComponent(
      () => of([{ id: 4, nom: 'KFOKAM48-2026' }]),
      () => of([review({ note: 12, nbNotes: 1, provisoire: true, commentaire: 'Correct.' })]),
      5,
    );

    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('12');
    expect(texte).toContain('1 note reçue sur 2');
    expect((fixture.nativeElement as HTMLElement).querySelector('.badge-provisoire')).not.toBeNull();
  });

  it('n’indique plus provisoire quand les deux pairs ont rendu', async () => {
    const fixture = await createComponent(
      () => of([{ id: 4, nom: 'KFOKAM48-2026' }]),
      () => of([review({ note: 14, nbNotes: 2, provisoire: false, commentaire: 'Très bien.' })]),
      5,
    );

    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('14');
    expect(texte).toContain('2 notes reçues sur 2');
    // Le badge, et non le texte d'explication qui emploie le mot.
    expect((fixture.nativeElement as HTMLElement).querySelector('.badge-provisoire')).toBeNull();
  });

  // EF9, Q13, RG12 — le bouton de remplacement n'est proposé que si la relecture n'a pas
  // commencé : le frontend ne doit pas inviter à une action que l'API refusera.
  it('propose de remplacer le lien tant que la relecture n’a pas commencé', async () => {
    const fixture = await createComponent(
      () => of([{ id: 4, nom: 'KFOKAM48-2026' }]),
      () => of([]),
      5,
    );
    fixture.componentInstance.replaceStatus.set('success');
    fixture.componentInstance.myExercises.set([
      {
        id: 6,
        etudiantId: 5,
        statut: 'EN_ATTENTE_DE_RELECTURE',
        relecteurs: [{ relectureId: 6, relecteurId: 7, commenceeAt: null, rendueAt: null }],
        noteRetenue: null,
        provisoire: true,
        relecteurId: 7,
        relectureId: 6,
        commenceeAt: null,
        rendueAt: null,
      },
    ]);
    fixture.detectChanges();

    const boutons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    );
    const remplacement = boutons.find((bouton) => bouton.textContent?.includes('Remplacer le lien'));
    expect(remplacement?.disabled).toBeFalsy();
  });

  it('ne propose pas de remplacer un lien quand la relecture a commencé', async () => {
    const fixture = await createComponent(
      () => of([{ id: 4, nom: 'KFOKAM48-2026' }]),
      () => of([]),
      5,
    );
    fixture.componentInstance.replaceStatus.set('success');
    fixture.componentInstance.myExercises.set([
      {
        id: 6,
        etudiantId: 5,
        statut: 'RELU',
        relecteurs: [
          { relectureId: 6, relecteurId: 7, commenceeAt: '2026-09-25T08:00:00Z', rendueAt: '2026-09-25T08:05:00Z' },
        ],
        noteRetenue: 15,
        provisoire: false,
        relecteurId: 7,
        relectureId: 6,
        commenceeAt: '2026-09-25T08:00:00Z',
        rendueAt: '2026-09-25T08:05:00Z',
      },
    ]);
    fixture.detectChanges();

    const boutons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    );
    const remplacement = boutons.find((bouton) => bouton.textContent?.includes('Remplacer le lien'));
    expect(remplacement?.disabled).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('n’est plus remplaçable');
  });

  it('affiche une attente plutôt qu’une note absente', async () => {
    const fixture = await createComponent(
      () => of([{ id: 4, nom: 'KFOKAM48-2026' }]),
      () => of([review({ note: null, nbNotes: 0, provisoire: true })]),
      5,
    );

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('en attente d’une note');
  });
});
