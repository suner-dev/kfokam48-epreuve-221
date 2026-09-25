import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  Etudiant,
  ExerciceSession,
  PresenceSession,
  Promotion,
  RequestError,
  RequestStatus,
  SessionDetail,
  SessionOuverte,
  StatutExercice,
  TableauEtudiant,
} from '../../core/models/api.models';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { ExercisesApiService } from '../../core/services/exercises-api.service';
import { toRequestError } from '../../core/services/api-error';
import { PresencesApiService } from '../../core/services/presences-api.service';
import { ReferencesApiService } from '../../core/services/references-api.service';
import { SessionsApiService } from '../../core/services/sessions-api.service';

@Component({
  selector: 'app-trainer-page',
  imports: [ReactiveFormsModule],
  templateUrl: './trainer-page.component.html',
  styleUrl: './trainer-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly referencesApi = inject(ReferencesApiService);
  private readonly sessionsApi = inject(SessionsApiService);
  private readonly presencesApi = inject(PresencesApiService);
  private readonly exercisesApi = inject(ExercisesApiService);
  private readonly dashboardApi = inject(DashboardApiService);

  readonly sessionForm = this.formBuilder.nonNullable.group({
    titre: ['', [Validators.required]],
    promotionId: [0, [Validators.min(1)]],
  });

  readonly manualPresenceForm = this.formBuilder.nonNullable.group({
    etudiantId: [0, [Validators.min(1)]],
  });

  readonly promotions = signal<Promotion[]>([]);
  readonly students = signal<Etudiant[]>([]);
  readonly dashboard = signal<TableauEtudiant[]>([]);
  readonly currentSession = signal<SessionDetail | null>(null);
  readonly presences = signal<PresenceSession[]>([]);
  readonly exercises = signal<ExerciceSession[]>([]);

  readonly promotionsStatus = signal<RequestStatus>('loading');
  readonly studentsStatus = signal<RequestStatus>('idle');
  readonly dashboardStatus = signal<RequestStatus>('idle');
  readonly sessionStatus = signal<RequestStatus>('idle');
  readonly presencesStatus = signal<RequestStatus>('idle');
  readonly exercisesStatus = signal<RequestStatus>('idle');
  readonly actionStatus = signal<RequestStatus>('idle');

  readonly promotionsError = signal<RequestError | null>(null);
  readonly studentsError = signal<RequestError | null>(null);
  readonly dashboardError = signal<RequestError | null>(null);
  readonly sessionError = signal<RequestError | null>(null);
  readonly presencesError = signal<RequestError | null>(null);
  readonly exercisesError = signal<RequestError | null>(null);
  readonly actionError = signal<RequestError | null>(null);
  readonly actionSuccess = signal<string | null>(null);

  constructor() {
    this.loadPromotions();
  }

  onPromotionChanged(): void {
    const promotionId = this.sessionForm.controls.promotionId.value;
    this.manualPresenceForm.controls.etudiantId.setValue(0);
    this.students.set([]);
    this.dashboard.set([]);

    if (promotionId < 1) {
      this.studentsStatus.set('idle');
      this.dashboardStatus.set('idle');
      return;
    }

    this.loadStudents(promotionId);
    this.loadDashboard(promotionId);
  }

  createSession(): void {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const { titre, promotionId } = this.sessionForm.getRawValue();
    this.beginAction();
    this.sessionsApi
      .createSession({ titre, promotionId })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => undefined),
      )
      .subscribe({
        next: (session) => {
          this.currentSession.set(this.toSessionDetail(session, titre, promotionId));
          this.actionStatus.set('success');
          this.actionSuccess.set('Session ouverte. Le code de présence est disponible ci-dessous.');
          this.loadSessionDetails(session.id);
          this.loadDashboard(promotionId);
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  endSession(): void {
    const session = this.currentSession();
    if (!session) {
      return;
    }

    this.beginAction();
    this.sessionsApi
      .endSession(session.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.currentSession.update((current) => (current ? { ...current, finAt: result.finAt } : null));
          this.actionStatus.set('success');
          this.actionSuccess.set('La session est terminée. Les étudiants ne peuvent plus marquer leur présence.');
          this.loadSessionDetails(session.id);
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  closeSession(): void {
    const session = this.currentSession();
    if (!session) {
      return;
    }

    this.beginAction();
    this.sessionsApi
      .closeSession(session.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.currentSession.update((current) =>
            current ? { ...current, finAt: result.finAt, clotureAt: result.clotureAt } : null,
          );
          this.actionStatus.set('success');
          this.actionSuccess.set('La session est clôturée et devient en lecture seule.');
          this.loadSessionDetails(session.id);
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  addManualPresence(): void {
    const session = this.currentSession();
    if (!session) {
      return;
    }

    if (this.manualPresenceForm.invalid) {
      this.manualPresenceForm.markAllAsTouched();
      return;
    }

    const etudiantId = this.manualPresenceForm.controls.etudiantId.value;
    this.beginAction();
    this.presencesApi
      .addManualPresence(session.id, etudiantId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionStatus.set('success');
          this.actionSuccess.set('La présence manuelle a été ajoutée avec la source FORMATEUR.');
          this.loadPresences(session.id);
          this.loadDashboard(session.promotionId);
        },
        error: (error: unknown) => this.failAction(error),
      });
  }

  refreshCurrentSession(): void {
    const session = this.currentSession();
    if (session) {
      this.loadSessionDetails(session.id);
      this.loadDashboard(session.promotionId);
    }
  }

  protected statusLabel(statut: StatutExercice): string {
    const labels: Record<StatutExercice, string> = {
      DEPOSE: 'Déposé',
      EN_ATTENTE_DE_RELECTURE: 'En attente de relecture',
      EN_ATTENTE_SANS_RELECTEUR: 'En attente sans relecteur',
      RELU: 'Relu',
      EN_ATTENTE_VERROUILLE: 'En attente verrouillée',
      RELU_VERROUILLE: 'Relu verrouillé',
    };
    return labels[statut];
  }

  protected statusClass(statut: StatutExercice): string {
    const classes: Record<StatutExercice, string> = {
      DEPOSE: 'status-depose',
      EN_ATTENTE_DE_RELECTURE: 'status-attente-relecture',
      EN_ATTENTE_SANS_RELECTEUR: 'status-attente-sans-relecteur',
      RELU: 'status-relu',
      EN_ATTENTE_VERROUILLE: 'status-attente-verrouillee',
      RELU_VERROUILLE: 'status-relu-verrouillee',
    };
    return classes[statut];
  }

  protected formatDate(value: string | null): string {
    if (!value) {
      return '—';
    }

    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
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
        },
        error: (error: unknown) => {
          this.studentsError.set(toRequestError(error));
          this.studentsStatus.set('error');
        },
      });
  }

  private loadDashboard(promotionId: number): void {
    this.dashboardStatus.set('loading');
    this.dashboardError.set(null);
    this.dashboardApi
      .getDashboard(promotionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (rows) => {
          this.dashboard.set(rows);
          this.dashboardStatus.set(rows.length ? 'success' : 'empty');
        },
        error: (error: unknown) => {
          this.dashboardError.set(toRequestError(error));
          this.dashboardStatus.set('error');
        },
      });
  }

  private loadSessionDetails(sessionId: number): void {
    this.loadSession(sessionId);
    this.loadPresences(sessionId);
    this.loadExercises(sessionId);
  }

  private loadSession(sessionId: number): void {
    this.sessionStatus.set('loading');
    this.sessionError.set(null);
    this.sessionsApi
      .getSession(sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.currentSession.set(session);
          this.sessionStatus.set('success');
        },
        error: (error: unknown) => {
          this.sessionError.set(toRequestError(error));
          this.sessionStatus.set('error');
        },
      });
  }

  private loadPresences(sessionId: number): void {
    this.presencesStatus.set('loading');
    this.presencesError.set(null);
    this.presencesApi
      .getSessionPresences(sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (presences) => {
          this.presences.set(presences);
          this.presencesStatus.set(presences.length ? 'success' : 'empty');
        },
        error: (error: unknown) => {
          this.presencesError.set(toRequestError(error));
          this.presencesStatus.set('error');
        },
      });
  }

  private loadExercises(sessionId: number): void {
    this.exercisesStatus.set('loading');
    this.exercisesError.set(null);
    this.exercisesApi
      .getSessionExercises(sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (exercises) => {
          this.exercises.set(exercises);
          this.exercisesStatus.set(exercises.length ? 'success' : 'empty');
        },
        error: (error: unknown) => {
          this.exercisesError.set(toRequestError(error));
          this.exercisesStatus.set('error');
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

  private toSessionDetail(session: SessionOuverte, titre: string, promotionId: number): SessionDetail {
    return { ...session, titre, promotionId, finAt: null, clotureAt: null };
  }
}
