import { Routes } from '@angular/router';
import { PlaceholderPageComponent } from './features/placeholder/placeholder-page.component';
import { StudentPageComponent } from './features/student/student-page.component';
import { TrainerPageComponent } from './features/trainer/trainer-page.component';

export const routes: Routes = [
  { path: 'formateur', component: TrainerPageComponent },
  { path: 'etudiant', component: StudentPageComponent },
  {
    path: 'relecteur',
    component: PlaceholderPageComponent,
    data: {
      title: 'Parcours relecteur',
      description: 'La liste des relectures assignées et le rendu de note arrivent dans le ticket dédié.',
    },
  },
  { path: '', pathMatch: 'full', redirectTo: 'formateur' },
  { path: '**', redirectTo: 'formateur' },
];
