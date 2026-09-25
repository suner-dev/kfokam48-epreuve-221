import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ExerciceMisAJour, ExerciceSession, StatutExercice } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ExercisesApiService {
  private readonly http = inject(HttpClient);

  submitExercise(sessionId: number, etudiantId: number, lien: string): Observable<{ id: number; statut: StatutExercice }> {
    return this.http.post<{ id: number; statut: StatutExercice }>('/api/exercices', {
      sessionId,
      etudiantId,
      lien,
    });
  }

  getSessionExercises(sessionId: number): Observable<ExerciceSession[]> {
    return this.http.get<ExerciceSession[]>(`/api/sessions/${sessionId}/exercices`);
  }

  /**
   * EF9, Q13, RG12 — l'étudiant remplace le lien de son exercice tant que la relecture
   * n'a pas commencé. L'API reste l'autorité : c'est elle qui renvoie
   * `409 RELECTURE_COMMENCEE` ou `410 SESSION_CLOTUREE`, le frontend n'invite pas à une
   * action interdite.
   */
  replaceExerciseLink(exerciceId: number, lien: string): Observable<ExerciceMisAJour> {
    return this.http.put<ExerciceMisAJour>(`/api/exercices/${exerciceId}`, { lien });
  }
}
