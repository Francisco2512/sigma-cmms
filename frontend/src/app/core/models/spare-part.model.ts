/** Refacción del inventario. */
export interface SparePart {
  id: number;
  sku: string;
  name: string;
  unit: string;
  stock: number;
  reorderPoint: number;
  unitCost: number;
  belowReorderPoint: boolean;
}
