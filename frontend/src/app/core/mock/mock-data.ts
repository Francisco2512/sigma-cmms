import { Asset, Kpis, PreventivePlan, SparePart, UserSummary, WorkOrderDetail, WorkOrderSummary } from '../models';

/** Fecha base de los datos simulados. */
export const HOY = new Date();

function fecha(offsetDias: number): string {
  const d = new Date(HOY);
  d.setDate(d.getDate() + offsetDias);
  return d.toISOString().slice(0, 10);
}

function instante(offsetHoras: number): string {
  const d = new Date(HOY);
  d.setHours(d.getHours() + offsetHoras);
  return d.toISOString().slice(0, 19);
}

export const USUARIOS: (UserSummary & { password: string })[] = [
  { id: 1, username: 'admin', fullName: 'Administrador SIGMA', role: 'ADMIN', password: 'sigma' },
  { id: 2, username: 'mruiz', fullName: 'Martha Ruiz', role: 'JEFE_MANTENIMIENTO', password: 'sigma' },
  { id: 3, username: 'jcastillo', fullName: 'Jorge Castillo', role: 'PLANIFICADOR', password: 'sigma' },
  { id: 4, username: 'lhernandez', fullName: 'Luis Hernández', role: 'TECNICO', password: 'sigma' },
  { id: 5, username: 'atorres', fullName: 'Ana Torres', role: 'TECNICO', password: 'sigma' },
  { id: 6, username: 'pgomez', fullName: 'Pedro Gómez', role: 'ALMACENISTA', password: 'sigma' },
];

export const ACTIVOS: Asset[] = [
  a(1, 'CMP-001', 'Compresor de tornillo 75 HP', 'Utilidades', 'ALTA', 'OPERATIVO', 'Atlas Copco'),
  a(2, 'CMP-002', 'Compresor reciprocante 30 HP', 'Utilidades', 'MEDIA', 'EN_MANTENIMIENTO', 'Ingersoll Rand'),
  a(3, 'CAL-001', 'Caldera pirotubular 150 BHP', 'Utilidades', 'ALTA', 'OPERATIVO', 'Cleaver-Brooks'),
  a(4, 'REF-001', 'Chiller de agua helada 80 TR', 'Utilidades', 'ALTA', 'OPERATIVO', 'Carrier'),
  a(5, 'BTR-001', 'Banda transportadora línea 1', 'Ensamble', 'ALTA', 'OPERATIVO', 'Hytrol'),
  a(6, 'BTR-002', 'Banda transportadora línea 2', 'Ensamble', 'MEDIA', 'OPERATIVO', 'Hytrol'),
  a(7, 'SOL-001', 'Robot de soldadura por puntos', 'Ensamble', 'ALTA', 'OPERATIVO', 'FANUC'),
  a(8, 'MON-001', 'Montacargas eléctrico 2.5 t', 'Ensamble', 'BAJA', 'OPERATIVO', 'Toyota'),
  a(9, 'HID-001', 'Prensa hidráulica 200 t', 'Estampado', 'ALTA', 'OPERATIVO', 'Schuler'),
  a(10, 'HID-002', 'Unidad hidráulica de potencia', 'Estampado', 'MEDIA', 'OPERATIVO', 'Parker'),
  a(11, 'TOR-001', 'Torno CNC', 'Maquinado', 'MEDIA', 'OPERATIVO', 'Haas'),
  a(12, 'REF-002', 'Cámara de refrigeración de materiales', 'Maquinado', 'BAJA', 'FUERA_DE_SERVICIO', 'Bohn'),
];

function a(id: number, code: string, name: string, area: string, criticality: Asset['criticality'],
           status: Asset['status'], manufacturer: string): Asset {
  return { id, code, name, area, location: `Nave ${area}`, criticality, status, manufacturer,
    model: null, commissionedAt: fecha(-1200) };
}

export const REFACCIONES: SparePart[] = [
  p(1, 'FLT-AIR-075', 'Filtro de aire para compresor 75 HP', 'pza', 6, 4, 850),
  p(2, 'ACE-CMP-20L', 'Aceite sintético para compresor 20 L', 'cubeta', 3, 2, 4200),
  p(3, 'ROD-6205', 'Rodamiento 6205-2RS', 'pza', 24, 10, 95.5),
  p(4, 'BND-A42', 'Banda en V A-42', 'pza', 8, 6, 180),
  p(5, 'KIT-SELL-HID', 'Kit de sellos para cilindro hidráulico', 'kit', 2, 2, 2350),
  p(6, 'MNG-HID-12', 'Manguera hidráulica 1/2 pulgada, 1 m', 'pza', 5, 4, 420),
  p(7, 'CNT-3P-32A', 'Contactor tripolar 32 A', 'pza', 4, 3, 1150),
  p(8, 'SNS-IND-M18', 'Sensor inductivo M18', 'pza', 3, 4, 690),
  p(9, 'GRS-EP2', 'Grasa EP2 multiusos, cartucho', 'pza', 30, 12, 85),
  p(10, 'REF-R134A', 'Refrigerante R-134a', 'kg', 12, 10, 310),
  p(11, 'ELC-SOLD-CU', 'Electrodo de cobre para soldadura', 'pza', 40, 25, 145),
  p(12, 'FUS-10A', 'Fusible 10 A tipo cartucho', 'pza', 50, 20, 18.5),
  p(13, 'VAL-SOL-24V', 'Válvula solenoide 24 VCD', 'pza', 1, 2, 1680),
  p(14, 'TRJ-PLC-ES', 'Tarjeta de entradas y salidas para PLC', 'pza', 1, 1, 7400),
];

function p(id: number, sku: string, name: string, unit: string, stock: number, reorderPoint: number,
           unitCost: number): SparePart {
  return { id, sku, name, unit, stock, reorderPoint, unitCost, belowReorderPoint: stock <= reorderPoint };
}

export const PLANES: PreventivePlan[] = [
  pl(1, 'Cambio de filtros y aceite', 1, 30, 0, 'ALTA', 'Cambiar filtro de aire y aceite; drenar condensados.'),
  pl(2, 'Inspección y purga de caldera', 3, 15, -2, 'ALTA', 'Purgar fondo, revisar tubos, probar válvula de seguridad.'),
  pl(3, 'Lubricación de rodamientos', 5, 14, 5, 'MEDIA', 'Engrasar chumaceras, verificar tensión de la banda.'),
  pl(4, 'Revisión de sellos hidráulicos', 9, 60, 20, 'ALTA', 'Inspeccionar sellos del cilindro y nivel de aceite.'),
  pl(5, 'Limpieza de condensador', 4, 30, 1, 'MEDIA', 'Lavar serpentín y verificar presiones de refrigerante.'),
  { ...pl(6, 'Calibración de trayectorias', 7, 90, 40, 'BAJA', 'Verificar puntos de referencia y electrodos.'),
    status: 'PAUSADO' },
];

function pl(id: number, name: string, assetId: number, frequencyDays: number, dueOffset: number,
            priority: PreventivePlan['priority'], taskDescription: string): PreventivePlan {
  const activo = ACTIVOS.find((x) => x.id === assetId)!;
  return { id, name, assetId, assetCode: activo.code, assetName: activo.name, frequencyDays,
    nextDueDate: fecha(dueOffset), taskDescription, priority, status: 'ACTIVO', due: dueOffset <= 0 };
}

export const ORDENES: WorkOrderSummary[] = [
  o(101, 'OT-2026-00101', 'Vibración excesiva en rodillo motriz', 'CORRECTIVA', 'ALTA', 'ABIERTA', 6, null, 0),
  o(102, 'OT-2026-00102', 'Fuga en conexión de manguera de presión', 'CORRECTIVA', 'MEDIA', 'ASIGNADA', 10, 4, 1),
  o(103, 'OT-2026-00103', 'Compresor no alcanza presión de trabajo', 'CORRECTIVA', 'ALTA', 'EN_PROCESO', 2, 4, 0),
  o(104, 'OT-2026-00104', 'Alarma de husillo por sobrecarga', 'CORRECTIVA', 'MEDIA', 'ASIGNADA', 11, 5, -2),
  o(105, 'OT-2026-00105', 'Preventivo: Limpieza de condensador', 'PREVENTIVA', 'MEDIA', 'ASIGNADA', 4, 5, 1),
  o(106, 'OT-2026-00106', 'Falla eléctrica en motor', 'CORRECTIVA', 'ALTA', 'CERRADA', 1, 4, -5),
  o(107, 'OT-2026-00107', 'Preventivo: Lubricación de rodamientos', 'PREVENTIVA', 'MEDIA', 'CERRADA', 5, 5, -8),
  o(108, 'OT-2026-00108', 'Pérdida de presión', 'CORRECTIVA', 'MEDIA', 'CERRADA', 9, 4, -12),
  o(109, 'OT-2026-00109', 'Ruido anormal en rodamiento', 'CORRECTIVA', 'BAJA', 'CERRADA', 8, 5, -18),
  o(110, 'OT-2026-00110', 'Reporte duplicado de falla', 'CORRECTIVA', 'BAJA', 'CANCELADA', 12, null, -20),
];

function o(id: number, code: string, title: string, type: WorkOrderSummary['type'],
           priority: WorkOrderSummary['priority'], status: WorkOrderSummary['status'], assetId: number,
           tecnicoId: number | null, dueOffset: number): WorkOrderSummary {
  const activo = ACTIVOS.find((x) => x.id === assetId)!;
  const tecnico = USUARIOS.find((u) => u.id === tecnicoId);
  const cerrada = status === 'CERRADA';
  return { id, code, title, type, priority, status, assetId, assetCode: activo.code, assetName: activo.name,
    assignedToId: tecnico?.id ?? null, assignedToName: tecnico?.fullName ?? null, dueDate: fecha(dueOffset),
    overdue: dueOffset < 0 && status !== 'CERRADA' && status !== 'CANCELADA',
    createdAt: instante(dueOffset * 24 - 6), closedAt: cerrada ? instante(dueOffset * 24 + 4) : null };
}

export const DETALLES = new Map<number, WorkOrderDetail>(
  ORDENES.map((resumen) => [resumen.id, {
    summary: resumen,
    description: 'Reporte levantado desde el piso de planta.',
    createdByName: 'Martha Ruiz',
    preventivePlanId: resumen.type === 'PREVENTIVA' ? 5 : null,
    failureAt: resumen.type === 'CORRECTIVA' ? resumen.createdAt : null,
    startedAt: ['EN_PROCESO', 'CERRADA'].includes(resumen.status) ? resumen.createdAt : null,
    laborHours: resumen.status === 'CERRADA' ? 2.5 : null,
    resolutionNotes: resumen.status === 'CERRADA' ? 'Se corrigió la falla y se probó el equipo.' : null,
    parts: resumen.status === 'CERRADA'
      ? [{ sparePartId: 3, sku: 'ROD-6205', name: 'Rodamiento 6205-2RS', quantity: 2, unitCost: 95.5, subtotal: 191 }]
      : [],
    partsCost: resumen.status === 'CERRADA' ? 191 : 0,
  }]),
);

export const KPIS: Kpis = {
  periodDays: 90, from: fecha(-90), to: fecha(0),
  mtbfHours: 742.5, mttrHours: 4.3, availabilityPct: 99.4, preventiveCompliancePct: 86.7,
  failures: 28, openOrders: 5, overdueOrders: 1, partsBelowReorder: 3,
  ordersByStatus: { ABIERTA: 1, ASIGNADA: 3, EN_PROCESO: 1, CERRADA: 61, CANCELADA: 1 },
  topAssets: [
    { assetId: 1, code: 'CMP-001', name: 'Compresor de tornillo 75 HP', failures: 6, mttrHours: 5.2, availabilityPct: 98.6 },
    { assetId: 5, code: 'BTR-001', name: 'Banda transportadora línea 1', failures: 5, mttrHours: 4.8, availabilityPct: 98.9 },
    { assetId: 9, code: 'HID-001', name: 'Prensa hidráulica 200 t', failures: 5, mttrHours: 6.1, availabilityPct: 98.3 },
    { assetId: 7, code: 'SOL-001', name: 'Robot de soldadura por puntos', failures: 4, mttrHours: 3.9, availabilityPct: 99.1 },
    { assetId: 3, code: 'CAL-001', name: 'Caldera pirotubular 150 BHP', failures: 3, mttrHours: 4.4, availabilityPct: 99.2 },
  ],
};
