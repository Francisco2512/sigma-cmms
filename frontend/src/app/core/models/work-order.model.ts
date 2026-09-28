import { Level } from './asset.model';

/** Ciclo de vida de una orden de trabajo. */
export type WorkOrderStatus = 'ABIERTA' | 'ASIGNADA' | 'EN_PROCESO' | 'CERRADA' | 'CANCELADA';

/** Origen de la orden. */
export type WorkOrderType = 'CORRECTIVA' | 'PREVENTIVA';

/** Fila del listado de órdenes. */
export interface WorkOrderSummary {
  id: number;
  code: string;
  title: string;
  type: WorkOrderType;
  priority: Level;
  status: WorkOrderStatus;
  assetId: number;
  assetCode: string;
  assetName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  dueDate: string;
  overdue: boolean;
  createdAt: string;
  closedAt: string | null;
}

/** Refacción consumida por una orden. */
export interface PartLine {
  sparePartId: number;
  sku: string;
  name: string;
  quantity: number;
  unitCost: number;
  subtotal: number;
}

/** Detalle completo de una orden. */
export interface WorkOrderDetail {
  summary: WorkOrderSummary;
  description: string | null;
  createdByName: string;
  preventivePlanId: number | null;
  failureAt: string | null;
  startedAt: string | null;
  laborHours: number | null;
  resolutionNotes: string | null;
  parts: PartLine[];
  partsCost: number;
}

/** Datos para crear una orden. */
export interface WorkOrderCreate {
  assetId: number;
  type: WorkOrderType;
  priority: Level;
  title: string;
  description?: string;
  dueDate: string;
  assignedToId?: number | null;
}

/** Datos para cerrar una orden. */
export interface WorkOrderClose {
  laborHours: number;
  resolutionNotes: string;
  parts: { sparePartId: number; quantity: number }[];
}

/** Etiquetas legibles de cada estatus. */
export const STATUS_LABELS: Record<WorkOrderStatus, string> = {
  ABIERTA: 'Abierta',
  ASIGNADA: 'Asignada',
  EN_PROCESO: 'En proceso',
  CERRADA: 'Cerrada',
  CANCELADA: 'Cancelada',
};
