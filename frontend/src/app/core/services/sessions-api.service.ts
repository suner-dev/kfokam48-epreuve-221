import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  SessionCloturee,
  SessionCreation,
  SessionDetail,
  SessionOuverte,
  SessionTerminee,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class SessionsApiService {
  private readonly http = inject(HttpClient);

  createSession(body: SessionCreation): Observable<SessionOuverte> {
    return this.http.post<SessionOuverte>('/api/sessions', body);
  }

  getSession(sessionId: number): Observable<SessionDetail> {
    return this.http.get<SessionDetail>(`/api/sessions/${sessionId}`);
  }

  endSession(sessionId: number): Observable<SessionTerminee> {
    return this.http.post<SessionTerminee>(`/api/sessions/${sessionId}/fin`, {});
  }

  closeSession(sessionId: number): Observable<SessionCloturee> {
    return this.http.post<SessionCloturee>(`/api/sessions/${sessionId}/cloture`, {});
  }
}
