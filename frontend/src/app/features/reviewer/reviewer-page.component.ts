import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Etudiant,
  Promotion,
  RelectureAFaire,
  RequestError,
  RequestStatus,
} from '../../core/models/api.models';
import { toRequestError } from '../../core/services/api-error';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { ReviewsApiService } from '../../core/services/reviews-api.service';

const integerValidator = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (value === null || value === '') {
    return { required: true };
  }
  return Number.isInteger(Number(value)) ? null : { integer: true };
};

@Component({
  selector: 'app-reviewer-page',
  imports: [ReactiveFormsModule],
  templateUrl: './reviewer-page.component.html',
  styleUrl: './reviewer-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewerPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly referencesApi = inject(ReferencesApiService);
  private readonly reviewsApi = inject(ReviewsApiService);

  readonly identityForm = this.formBuilder.nonNullable.group({
    promotionId: [0, [Validators.min(1)]],
    etudiantId: [{ value: 0, disabled: true }, [Validators.min(1)]],
  });

  /** EF10 — les mêmes champs que le rendu, corrigés après coup. */
  readonly correctionForm = this.formBuilder.nonNullable.group({
    note: [0, [Validators.required, Validators.min(0), Validators.max(20), integerValidator]],
    commentaire: ['', []],
  });

  readonly reviewForm = this.formBuilder.nonNullable.group({
    note: [0, [integerValidator, Validators.min(0), Validators.max(20)]],
    commentaire: ['', [Validators.required]],
  });

  readonly promotions = signal<Promotion[]>([]);
  readonly students = signal<Etudiant[]>([]);
  readonly pendingReviews = signal<RelectureAFaire[]>([]);
  readonly activeReview = signal<RelectureAFaire | null>(null);
  /**
   * EF10, Q10, RG9 — la relecture que je viens de rendre dans cette visite. Aucune opération
   * du contrat ne liste les relectures DÉJÀ rendues par un relecteur donné, et inventer un
   * endpoint pour les retrouver serait sort du contrat. La correction est donc offerte sur
   * la seule relecture dont l'identifiant est déjà en mémoire, ce que l'issue #64 autorise
   * explicitement et déclare comme limite.
   */
  readonly lastRenderedReview = signal<{ relectureId: number; note: number; commentaire: string } | null>(null);

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
    this.pendingReviews.set([]);
    this.activeReview.set(null);
    this.reviewsStatus.set('idle');

    if (promotionId < 1) {
      this.studentsStatus.set('idle');
      return;
    }

    this.loadStudents(promotionId);
  }

  onStudentChanged(): void {
    const etudiantId = this.identityForm.controls.etudiantId.value;
    this.activeReview.set(null);
    this.pendingReviews.set([]);

    if (etudiantId < 1) {
      this.reviewsStatus.set('idle');
      return;
    }

    this.loadPendingReviews(etudiantId);
  }

  startReview(review: RelectureAFaire): void {
    if (review.commenceeAt) {
      this.activeReview.set(review);
      this.actionStatus.set('success');
      this.actionError.set(null);
      this.actionSuccess.set('Cette relecture est déjà commencée. Vous pouvez rendre votre note.');
      return;
    }

    this.beginAction();
    this.reviewsApi
      .startReview(review.relectureId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (started) => {
          this.activeReview.set({ ...review, commenceeAt: started.commenceeAt });
          this.actionStatus.set('success');
          this.actionSuccess.set('La relecture est commencée. Le lien de l’exercice est maintenant consultable.');
          const etudiantId = this.identityForm.controls.etudiantId.value;
          if (etudiantId > 0) {
            this.loadPendingReviews(etudiantId);
          }
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  submitReview(): void {
    const activeReview = this.activeReview();
    if (!activeReview) {
      this.actionError.set({ message: 'Commencez ou sélectionnez une relecture avant d’envoyer une note.', network: false });
      this.actionStatus.set('error');
      return;
    }

    if (this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }

    const body = this.reviewForm.getRawValue();
    this.beginAction();
    this.reviewsApi
      .submitReview(activeReview.relectureId, body)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.reviewForm.reset({ note: 0, commentaire: '' });
          this.activeReview.set(null);
          this.actionStatus.set('success');
          this.actionSuccess.set('Votre note et votre commentaire ont été enregistrés.');
          // On garde la relecture rendue en mémoire pour proposer sa correction (EF10).
          this.lastRenderedReview.set({
            relectureId: activeReview.relectureId,
            note: body.note,
            commentaire: body.commentaire,
          });
          this.correctionForm.setValue({ note: body.note, commentaire: body.commentaire });
          const etudiantId = this.identityForm.controls.etudiantId.value;
          if (etudiantId > 0) {
            this.loadPendingReviews(etudiantId);
          }
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  /**
   * EF10 — un nouveau PUT annule la valeur précédente. Le backend, seul juge de la clôture,
   * peut refuser : l'erreur est alors affichée telle quelle.
   */
  correctReview(): void {
    const last = this.lastRenderedReview();
    if (!last) {
      return;
    }
    if (this.correctionForm.invalid) {
      this.correctionForm.markAllAsTouched();
      return;
    }
    const body = this.correctionForm.getRawValue();
    this.actionStatus.set('loading');
    this.actionError.set(null);
    this.actionSuccess.set(null);
    this.reviewsApi
      .correctReview(last.relectureId, body)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionStatus.set('success');
          this.actionSuccess.set(`Note corrigée : ${body.note}/20.`);
          this.lastRenderedReview.set({ relectureId: last.relectureId, note: body.note, commentaire: body.commentaire });
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  refreshPendingReviews(): void {
    const etudiantId = this.identityForm.controls.etudiantId.value;
    if (etudiantId > 0) {
      this.loadPendingReviews(etudiantId);
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

  private loadPendingReviews(etudiantId: number): void {
    this.reviewsStatus.set('loading');
    this.reviewsError.set(null);
    this.reviewsApi
      .getPendingReviews(etudiantId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reviews) => {
          this.pendingReviews.set(reviews);
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
