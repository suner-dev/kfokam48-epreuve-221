import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ExerciceSession, StatutExercice } from '../models/api.models';

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
}
