import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      // Les transitions de route relies sur la View Transitions API. Le
      // navigateur qui ne l'implemente pas ignore l'option : la navigation
      // reste immediate et fonctionnelle, sans repli a ecrire.
      withViewTransitions(),
    ),
  ],
};
