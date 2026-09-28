import { Level } from './asset.model';
import { WorkOrderSummary } from './work-order.model';

/** Un plan pausado no genera órdenes. */
export type PlanStatus = 'ACTIVO' | 'PAUSADO';

/** Plan de mantenimiento preventivo. */
export interface PreventivePlan {
  id: number;
  name: string;
  assetId: number;
  assetCode: string;
  assetName: string;
  frequencyDays: number;
  nextDueDate: string;
  taskDescription: string;
  priority: Level;
  status: PlanStatus;
  due: boolean;
}

/** Datos para crear un plan. */
export interface PlanCreate {
  name: string;
  assetId: number;
  frequencyDays: number;
  nextDueDate: string;
  taskDescription: string;
  priority: Level;
}

/** Resultado de la generación de órdenes preventivas. */
export interface GenerationResult {
  generated: number;
  skippedCycles: number;
  orders: WorkOrderSummary[];
}
