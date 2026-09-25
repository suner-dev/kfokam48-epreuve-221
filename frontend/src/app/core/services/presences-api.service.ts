import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Presence, PresenceSession } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PresencesApiService {
  private readonly http = inject(HttpClient);

  markPresence(code: string, etudiantId: number): Observable<Presence> {
    return this.http.post<Presence>('/api/presences', { code, etudiantId });
  }

  addManualPresence(sessionId: number, etudiantId: number): Observable<Presence> {
    return this.http.post<Presence>('/api/presences/manuelle', { sessionId, etudiantId });
  }

  getSessionPresences(sessionId: number): Observable<PresenceSession[]> {
    return this.http.get<PresenceSession[]>(`/api/sessions/${sessionId}/presences`);
  }
}
