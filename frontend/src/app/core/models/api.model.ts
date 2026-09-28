/** Envoltura estándar de las respuestas exitosas del backend. */
export interface ApiResult<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

/** Página de resultados (paginación por desplazamiento). */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Error RFC 7807 devuelto por el backend. */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}
