import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Etudiant,
  ExerciceSession,
  Promotion,
  RelectureRecue,
  RequestError,
  RequestStatus,
} from '../../core/models/api.models';
import { toRequestError } from '../../core/services/api-error';
import { ExercisesApiService } from '../../core/services/exercises-api.service';
import { PresencesApiService } from '../../core/services/presences-api.service';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { ReviewsApiService } from '../../core/services/reviews-api.service';

@Component({
  selector: 'app-student-page',
  imports: [ReactiveFormsModule],
  templateUrl: './student-page.component.html',
  styleUrl: './student-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly referencesApi = inject(ReferencesApiService);
  private readonly presencesApi = inject(PresencesApiService);
  private readonly exercisesApi = inject(ExercisesApiService);
  private readonly reviewsApi = inject(ReviewsApiService);

  readonly identityForm = this.formBuilder.nonNullable.group({
    promotionId: [0, [Validators.min(1)]],
    etudiantId: [{ value: 0, disabled: true }, [Validators.min(1)]],
  });

  readonly presenceForm = this.formBuilder.nonNullable.group({
    code: ['', [Validators.required]],
  });

  readonly exerciseForm = this.formBuilder.nonNullable.group({
    sessionId: [0, [Validators.min(1)]],
    lien: ['', [Validators.required]],
  });

  /** EF9, Q13, RG12 — un seul lien remplacé à la fois, celui de l'exercice choisi. */
  readonly replaceForm = this.formBuilder.nonNullable.group({
    exerciceId: [0, [Validators.min(1)]],
    lien: ['', [Validators.required]],
  });

  readonly promotions = signal<Promotion[]>([]);
  readonly students = signal<Etudiant[]>([]);
  readonly receivedReviews = signal<RelectureRecue[]>([]);
  readonly myExercises = signal<ExerciceSession[]>([]);
  readonly replaceStatus = signal<RequestStatus>('idle');
  readonly replaceError = signal<RequestError | null>(null);
  readonly replaceSuccess = signal<string | null>(null);

  readonly promotionsStatus = signal<RequestStatus>('loading');
  readonly studentsStatus = signal<RequestStatus>('idle');
  readonly reviewsStatus = signal<RequestStatus>('idle');
  readonly actionStatus = signal<RequestStatus>('idle');

  readonly promotionsError = signal<RequestError | null>(null);
  readonly studentsError = signal<RequestError | null>(null);
  readonly reviewsError = signal<RequestError | null>(null);
  readonly actionError = signal<RequestError | null>(null);
  readonly actionSuccess = signal<string | null>(null);

  constructor() {
    this.loadPromotions();
  }

  onPromotionChanged(): void {
    const promotionId = this.identityForm.controls.promotionId.value;
    this.identityForm.controls.etudiantId.reset({ value: 0, disabled: true });
    this.students.set([]);
    this.receivedReviews.set([]);
    this.reviewsStatus.set('idle');

    if (promotionId < 1) {
      this.studentsStatus.set('idle');
      return;
    }

    this.loadStudents(promotionId);
  }

  onStudentChanged(): void {
    const etudiantId = this.identityForm.controls.etudiantId.value;
    this.receivedReviews.set([]);

    if (etudiantId < 1) {
      this.reviewsStatus.set('idle');
      return;
    }

    this.loadReceivedReviews(etudiantId);
  }

  markPresence(): void {
    if (this.identityForm.controls.etudiantId.invalid || this.presenceForm.invalid) {
      this.identityForm.controls.etudiantId.markAsTouched();
      this.presenceForm.markAllAsTouched();
      return;
    }

    const etudiantId = this.identityForm.controls.etudiantId.value;
    const code = this.presenceForm.controls.code.value;
    this.beginAction();

    this.presencesApi
      .markPresence(code, etudiantId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (presence) => {
          this.exerciseForm.controls.sessionId.setValue(presence.sessionId);
          this.actionStatus.set('success');
          this.actionSuccess.set('Votre présence a été enregistrée. L’identifiant de session est prêt pour le dépôt.');
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  submitExercise(): void {
    if (this.identityForm.controls.etudiantId.invalid || this.exerciseForm.invalid) {
      this.identityForm.controls.etudiantId.markAsTouched();
      this.exerciseForm.markAllAsTouched();
      return;
    }

    const etudiantId = this.identityForm.controls.etudiantId.value;
    const { sessionId, lien } = this.exerciseForm.getRawValue();
    this.beginAction();

    this.exercisesApi
      .submitExercise(sessionId, etudiantId, lien)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (exercise) => {
          this.actionStatus.set('success');
          this.actionSuccess.set(`Votre exercice est déposé avec le statut ${exercise.statut}.`);
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  /**
   * Source de données autorisée, sans inventer d'endpoint (issue #63) : l'API ne propose
   * que le détail par session, on filtre donc sur l'étudiant choisi. Le filtre est de
   * présentation, il ne sert qu'a masquer au lecteur les exercices des autres.
   */
  loadMyExercises(sessionId: number): void {
    const etudiantId = this.identityForm.controls.etudiantId.value;
    if (sessionId < 1 || etudiantId < 1) {
      this.myExercises.set([]);
      return;
    }
    this.replaceStatus.set('loading');
    this.replaceError.set(null);
    this.exercisesApi
      .getSessionExercises(sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (exercices) => {
          this.myExercises.set(exercices.filter((exercice) => exercice.etudiantId === etudiantId));
          this.replaceStatus.set(this.myExercises().length > 0 ? 'success' : 'empty');
        },
        error: (error: unknown) => {
          this.myExercises.set([]);
          this.replaceStatus.set('error');
          this.replaceError.set(toRequestError(error));
        },
      });
  }

  onMyExercisesRequested(): void {
    this.replaceSuccess.set(null);
    this.loadMyExercises(this.exerciseForm.controls.sessionId.value);
  }

  replaceLink(exerciceId: number): void {
    const { lien } = this.replaceForm.getRawValue();
    this.replaceForm.controls.lien.setValue(lien);
    this.replaceStatus.set('loading');
    this.replaceError.set(null);
    this.replaceSuccess.set(null);
    this.exercisesApi
      .replaceExerciseLink(exerciceId, lien)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (exercice) => {
          this.replaceStatus.set('success');
          this.replaceSuccess.set(`Lien mis à jour. Le statut reste ${exercice.statut}.`);
          this.loadMyExercises(this.exerciseForm.controls.sessionId.value);
        },
        error: (error: unknown) => {
          // 409 RELECTURE_COMMENCEE et 410 SESSION_CLOTUREE sont affichés tels quels.
          this.replaceStatus.set('error');
          this.replaceError.set(toRequestError(error));
        },
      });
  }

  refreshReceivedReviews(): void {
    const etudiantId = this.identityForm.controls.etudiantId.value;
    if (etudiantId > 0) {
      this.loadReceivedReviews(etudiantId);
    }
  }

  private loadPromotions(): void {
    this.promotionsStatus.set('loading');
    this.promotionsError.set(null);
    this.referencesApi
      .getPromotions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (promotions) => {
          this.promotions.set(promotions);
          this.promotionsStatus.set(promotions.length ? 'success' : 'empty');
        },
        error: (error: unknown) => {
          this.promotionsError.set(toRequestError(error));
          this.promotionsStatus.set('error');
        },
      });
  }

  private loadStudents(promotionId: number): void {
    this.studentsStatus.set('loading');
    this.studentsError.set(null);
    this.referencesApi
      .getEtudiants(promotionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (students) => {
          this.students.set(students);
          this.studentsStatus.set(students.length ? 'success' : 'empty');
          if (students.length) {
            this.identityForm.controls.etudiantId.enable({ emitEvent: false });
          } else {
            this.identityForm.controls.etudiantId.disable({ emitEvent: false });
          }
        },
        error: (error: unknown) => {
          this.identityForm.controls.etudiantId.disable({ emitEvent: false });
          this.studentsError.set(toRequestError(error));
          this.studentsStatus.set('error');
        },
      });
  }

  private loadReceivedReviews(etudiantId: number): void {
    this.reviewsStatus.set('loading');
    this.reviewsError.set(null);
    this.reviewsApi
      .getReceivedReviews(etudiantId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reviews) => {
          this.receivedReviews.set(reviews);
          this.reviewsStatus.set(reviews.length ? 'success' : 'empty');
        },
        error: (error: unknown) => {
          this.reviewsError.set(toRequestError(error));
          this.reviewsStatus.set('error');
        },
      });
  }

  private beginAction(): void {
    this.actionStatus.set('loading');
    this.actionError.set(null);
    this.actionSuccess.set(null);
  }

  private failAction(error: unknown): void {
    this.actionError.set(toRequestError(error));
    this.actionStatus.set('error');
  }
}
