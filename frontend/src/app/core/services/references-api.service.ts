import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Etudiant, Promotion } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ReferencesApiService {
  private readonly http = inject(HttpClient);

  getPromotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>('/api/promotions');
  }

  getEtudiants(promotionId: number): Observable<Etudiant[]> {
    const params = new HttpParams().set('promotionId', promotionId);
    return this.http.get<Etudiant[]>('/api/etudiants', { params });
  }
}
