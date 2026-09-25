import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TableauEtudiant } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);

  getDashboard(promotionId: number): Observable<TableauEtudiant[]> {
    const params = new HttpParams().set('promotionId', promotionId);
    return this.http.get<TableauEtudiant[]>('/api/tableau', { params });
  }
}
