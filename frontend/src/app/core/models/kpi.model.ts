/** Indicadores de un activo en el periodo. */
export interface AssetKpi {
  assetId: number;
  code: string;
  name: string;
  failures: number;
  mttrHours: number | null;
  availabilityPct: number | null;
}

/** Tablero de indicadores. Un valor nulo significa "sin datos suficientes". */
export interface Kpis {
  periodDays: number;
  from: string;
  to: string;
  mtbfHours: number | null;
  mttrHours: number | null;
  availabilityPct: number | null;
  preventiveCompliancePct: number | null;
  failures: number;
  openOrders: number;
  overdueOrders: number;
  partsBelowReorder: number;
  ordersByStatus: Record<string, number>;
  topAssets: AssetKpi[];
}
