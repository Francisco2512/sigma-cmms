/** Estado operativo de un activo. */
export type AssetStatus = 'OPERATIVO' | 'EN_MANTENIMIENTO' | 'FUERA_DE_SERVICIO';

/** Nivel de impacto; también se usa como prioridad. */
export type Level = 'ALTA' | 'MEDIA' | 'BAJA';

/** Activo del catálogo. */
export interface Asset {
  id: number;
  code: string;
  name: string;
  area: string;
  location: string | null;
  criticality: Level;
  status: AssetStatus;
  manufacturer: string | null;
  model: string | null;
  commissionedAt: string | null;
}
