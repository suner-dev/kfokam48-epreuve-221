import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  RelectureAFaire,
  RelectureCommencee,
  RelectureRecue,
  SoumissionRelecture,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ReviewsApiService {
  private readonly http = inject(HttpClient);

  getReceivedReviews(etudiantId: number): Observable<RelectureRecue[]> {
    return this.http.get<RelectureRecue[]>(`/api/etudiants/${etudiantId}/relectures-recues`);
  }

  getPendingReviews(etudiantId: number): Observable<RelectureAFaire[]> {
    const params = new HttpParams().set('etudiantId', etudiantId);
    return this.http.get<RelectureAFaire[]>('/api/relectures/a-faire', { params });
  }

  startReview(relectureId: number): Observable<RelectureCommencee> {
    return this.http.post<RelectureCommencee>(`/api/relectures/${relectureId}/debut`, {});
  }

  submitReview(relectureId: number, body: SoumissionRelecture): Observable<void> {
    return this.http.post<void>(`/api/relectures/${relectureId}`, body);
  }
}
