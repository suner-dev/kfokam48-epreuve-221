import { Routes } from '@angular/router';
import { ReviewerPageComponent } from './features/reviewer/reviewer-page.component';
import { StudentPageComponent } from './features/student/student-page.component';
import { TrainerPageComponent } from './features/trainer/trainer-page.component';

export const routes: Routes = [
  { path: 'formateur', component: TrainerPageComponent },
  { path: 'etudiant', component: StudentPageComponent },
  { path: 'relecteur', component: ReviewerPageComponent },
  { path: '', pathMatch: 'full', redirectTo: 'formateur' },
  { path: '**', redirectTo: 'formateur' },
];
