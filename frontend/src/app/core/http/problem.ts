import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../models';

/**
 * Traduce un error HTTP a un mensaje para el usuario, usando el ProblemDetail del backend.
 * @param error error capturado
 * @returns mensaje legible
 */
export function problemMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado';
  }
  if (error.status === 0) {
    return 'No hay conexión con el servidor';
  }
  const problem = error.error as ProblemDetail | null;
  const fieldErrors = problem?.errors ? Object.values(problem.errors) : [];
  if (fieldErrors.length > 0) {
    return fieldErrors.join('. ');
  }
  return problem?.detail ?? problem?.title ?? `Error ${error.status}`;
}
