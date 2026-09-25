export interface ApiError {
  code: string;
  message: string;
}

export interface RequestError {
  code?: string;
  message: string;
  network: boolean;
}

export type RequestStatus = 'idle' | 'loading' | 'success' | 'empty' | 'error';

export interface Promotion {
  id: number;
  nom: string;
}

export interface Etudiant {
  id: number;
  nom: string;
  prenom: string;
}

export interface SessionCreation {
  titre: string;
  promotionId: number;
}

export interface SessionOuverte {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
}

export interface SessionDetail extends SessionOuverte {
  titre: string;
  promotionId: number;
  finAt: string | null;
  clotureAt: string | null;
}

export interface SessionTerminee {
  id: number;
  finAt: string;
}

export interface SessionCloturee extends SessionTerminee {
  clotureAt: string;
}

export type SourcePresence = 'ETUDIANT' | 'FORMATEUR';

export interface Presence {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: SourcePresence;
}

export interface PresenceSession {
  etudiantId: number;
  nom: string;
  present: boolean;
  source: SourcePresence | null;
  marqueeAt: string | null;
}

export type StatutExercice =
  | 'DEPOSE'
  | 'EN_ATTENTE_DE_RELECTURE'
  | 'EN_ATTENTE_SANS_RELECTEUR'
  | 'RELU'
  | 'EN_ATTENTE_VERROUILLE'
  | 'RELU_VERROUILLE';

export interface ExerciceSession {
  id: number;
  etudiantId: number;
  statut: StatutExercice;
  relecteurId: number | null;
  commenceeAt: string | null;
  rendueAt: string | null;
}

export interface TableauEtudiant {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  moyenne: number | null;
  relecturesEnAttente: number;
}

export interface RelectureRecue {
  exerciceId: number;
  sessionId: number;
  lienExercice: string;
  note: number;
  commentaire: string;
}

export interface RelectureAFaire {
  relectureId: number;
  exerciceId: number;
  lienExercice: string;
  commenceeAt: string | null;
}

export interface RelectureCommencee {
  id: number;
  commenceeAt: string;
}

export interface SoumissionRelecture {
  note: number;
  commentaire: string;
}
