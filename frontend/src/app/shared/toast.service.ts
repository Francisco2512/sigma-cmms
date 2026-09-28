import { Injectable, signal } from '@angular/core';

/** Tipo visual de una notificación. */
export type ToastKind = 'success' | 'error' | 'info';

/** Notificación temporal. */
export interface Toast {
  id: number;
  kind: ToastKind;
  text: string;
}

const DURATION_MS = 4500;

/** Notificaciones breves en la esquina de la pantalla. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly items = signal<Toast[]>([]);

  /** Notificaciones visibles. */
  readonly toasts = this.items.asReadonly();

  /** @param text mensaje de éxito */
  success(text: string): void {
    this.show('success', text);
  }

  /** @param text mensaje de error */
  error(text: string): void {
    this.show('error', text);
  }

  /** @param id notificación a cerrar */
  dismiss(id: number): void {
    this.items.update((list) => list.filter((toast) => toast.id !== id));
  }

  private show(kind: ToastKind, text: string): void {
    const id = this.nextId++;
    this.items.update((list) => [...list, { id, kind, text }]);
    setTimeout(() => this.dismiss(id), DURATION_MS);
  }
}
