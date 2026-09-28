import {
  ApplicationConfig,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { PreloadAllModules, provideRouter, withComponentInputBinding, withPreloading } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth';
import { mockInterceptor } from './core/mock/mock.interceptor';

registerLocaleData(localeEsMx);

/** true: la app funciona sin backend con datos simulados (solo para demostración). */
export const USAR_DATOS_SIMULADOS = false;

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withPreloading(PreloadAllModules), withComponentInputBinding()),
    provideHttpClient(withInterceptors(
      USAR_DATOS_SIMULADOS ? [authInterceptor, mockInterceptor] : [authInterceptor],
    )),
    { provide: LOCALE_ID, useValue: 'es-MX' },
  ],
};
