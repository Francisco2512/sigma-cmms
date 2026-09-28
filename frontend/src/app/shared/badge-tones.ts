import { AssetStatus, Level, STATUS_LABELS, WorkOrderStatus } from '../core/models';
import { BadgeTone } from './badge.component';

/** Tono por estatus de orden. */
export const STATUS_TONES: Record<WorkOrderStatus, BadgeTone> = {
  ABIERTA: 'info',
  ASIGNADA: 'warning',
  EN_PROCESO: 'progress',
  CERRADA: 'success',
  CANCELADA: 'neutral',
};

/** Tono por nivel de prioridad o criticidad. */
export const LEVEL_TONES: Record<Level, BadgeTone> = { ALTA: 'danger', MEDIA: 'warning', BAJA: 'neutral' };

/** Tono y etiqueta por estado del activo. */
export const ASSET_STATUS: Record<AssetStatus, { label: string; tone: BadgeTone }> = {
  OPERATIVO: { label: 'Operativo', tone: 'success' },
  EN_MANTENIMIENTO: { label: 'En mantenimiento', tone: 'progress' },
  FUERA_DE_SERVICIO: { label: 'Fuera de servicio', tone: 'danger' },
};

/** Etiqueta legible de un nivel. */
export const LEVEL_LABELS: Record<Level, string> = { ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' };

export { STATUS_LABELS };
