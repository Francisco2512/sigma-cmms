import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { Observable, delay, of, throwError } from 'rxjs';
import { ApiResult, PageResponse, UserSummary, WorkOrderDetail, WorkOrderSummary } from '../models';
import { ACTIVOS, DETALLES, KPIS, ORDENES, PLANES, REFACCIONES, USUARIOS } from './mock-data';

/**
 * Responde las llamadas a la API con datos en memoria. Solo se registra cuando
 * `USAR_DATOS_SIMULADOS` está activo (ver app.config.ts), para demostrar la aplicación
 * sin levantar el backend. En el repositorio queda desactivado.
 */
export const mockInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  const url = new URL('http://mock' + req.urlWithParams);
  const ruta = url.pathname.replace('/api/v1', '');
  const q = url.searchParams;
  const cuerpo = req.body as Record<string, never> | null;

  try {
    const datos = resolver(req.method, ruta, q, cuerpo);
    return datos === undefined ? next(req) : responder(datos);
  } catch (error) {
    return throwError(() => error as HttpErrorResponse);
  }
};

let sesion: UserSummary = USUARIOS[1];
let siguienteId = 200;

function responder<T>(data: T): Observable<HttpResponse<ApiResult<T>>> {
  const body: ApiResult<T> = { success: true, data, message: null, timestamp: new Date().toISOString() };
  return of(new HttpResponse({ status: 200, body })).pipe(delay(120));
}

function error(status: number, detail: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { status, detail, title: 'Error' } });
}

function pagina<T>(items: T[], q: URLSearchParams): PageResponse<T> {
  const size = Number(q.get('size') ?? 15);
  const number = Number(q.get('page') ?? 0);
  const desde = number * size;
  return { content: items.slice(desde, desde + size), totalElements: items.length,
    totalPages: Math.max(1, Math.ceil(items.length / size)), number, size };
}

function texto(valor: string | null): string {
  return (valor ?? '').trim().toLowerCase();
}

function resumen(id: number): WorkOrderSummary {
  const encontrada = ORDENES.find((o) => o.id === id);
  if (!encontrada) {
    throw error(404, 'La orden no existe');
  }
  return encontrada;
}

function detalle(id: number): WorkOrderDetail {
  const d = DETALLES.get(id);
  if (!d) {
    throw error(404, 'La orden no existe');
  }
  return { ...d, summary: resumen(id) };
}

/** Devuelve `undefined` cuando la ruta no está simulada (la petición sigue al backend). */
// eslint-disable-next-line complexity
function resolver(metodo: string, ruta: string, q: URLSearchParams, cuerpo: Record<string, never> | null): unknown {
  const partes = ruta.split('/').filter(Boolean);
  const id = Number(partes[1]);
  const accion = partes[2];

  if (ruta === '/auth/login') {
    // Modo demostración: cualquier usuario y contraseña entran. Si el usuario coincide con
    // uno del catálogo se toma su rol; si no, se entra como jefa de mantenimiento.
    const ingresado = String(cuerpo?.['username'] ?? '').trim().toLowerCase();
    const usuario = USUARIOS.find((u) => u.username.toLowerCase() === ingresado) ?? USUARIOS[1];
    sesion = usuario;
    const expira = new Date(Date.now() + 2 * 3600 * 1000).toISOString();
    return { token: 'demo-' + usuario.username, expiresAt: expira, user: usuario };
  }
  if (ruta === '/auth/me') {
    return sesion;
  }
  if (ruta === '/users') {
    return USUARIOS.filter((u) => u.role === (q.get('role') ?? 'TECNICO'));
  }
  if (ruta === '/dashboard/kpis') {
    const dias = Number(q.get('days') ?? 90);
    const factor = dias / 90;
    return { ...KPIS, periodDays: dias, failures: Math.max(1, Math.round(KPIS.failures * factor)),
      mtbfHours: Math.round(KPIS.mtbfHours! * factor * 10) / 10 };
  }

  if (partes[0] === 'assets') {
    if (metodo === 'GET' && partes.length === 1) {
      const busqueda = texto(q.get('search'));
      const estado = q.get('status');
      return pagina(ACTIVOS.filter((a) =>
        (!busqueda || a.code.toLowerCase().includes(busqueda) || a.name.toLowerCase().includes(busqueda))
        && (!estado || a.status === estado)), q);
    }
    if (metodo === 'GET' && accion === 'work-orders') {
      return pagina(ORDENES.filter((o) => o.assetId === id), q);
    }
    if (metodo === 'GET') {
      const activo = ACTIVOS.find((a) => a.id === id);
      if (!activo) {
        throw error(404, 'El activo no existe');
      }
      return activo;
    }
  }

  if (partes[0] === 'spare-parts') {
    if (metodo === 'GET') {
      const busqueda = texto(q.get('search'));
      const soloAlertas = q.get('belowReorder') === 'true';
      return pagina(REFACCIONES.filter((r) =>
        (!busqueda || r.sku.toLowerCase().includes(busqueda) || r.name.toLowerCase().includes(busqueda))
        && (!soloAlertas || r.belowReorderPoint)), q);
    }
    if (metodo === 'PATCH' && accion === 'stock') {
      const parte = REFACCIONES.find((r) => r.id === id)!;
      parte.stock += Number(cuerpo?.['quantity'] ?? 0);
      parte.belowReorderPoint = parte.stock <= parte.reorderPoint;
      return parte;
    }
  }

  if (partes[0] === 'preventive-plans') {
    if (metodo === 'GET') {
      return [...PLANES].sort((x, y) => x.nextDueDate.localeCompare(y.nextDueDate));
    }
    if (metodo === 'POST' && accion === undefined && partes.length === 1) {
      const activo = ACTIVOS.find((a) => a.id === Number(cuerpo?.['assetId']))!;
      const plan = { id: siguienteId++, ...(cuerpo as object), assetCode: activo.code, assetName: activo.name,
        status: 'ACTIVO', due: false } as (typeof PLANES)[number];
      PLANES.push(plan);
      return plan;
    }
    if (metodo === 'PATCH' && accion === 'status') {
      const plan = PLANES.find((x) => x.id === id)!;
      plan.status = cuerpo?.['status'] as unknown as typeof plan.status;
      return plan;
    }
    if (metodo === 'POST' && partes[1] === 'generate') {
      const vencidos = PLANES.filter((x) => x.status === 'ACTIVO' && x.due);
      const generadas = vencidos.map((plan) => nuevaOrden({
        assetId: plan.assetId, type: 'PREVENTIVA', priority: plan.priority,
        title: 'Preventivo: ' + plan.name, dueDate: plan.nextDueDate, assignedToId: null,
      }));
      vencidos.forEach((plan) => {
        plan.due = false;
        const proxima = new Date();
        proxima.setDate(proxima.getDate() + plan.frequencyDays);
        plan.nextDueDate = proxima.toISOString().slice(0, 10);
      });
      return { generated: generadas.length, skippedCycles: 0, orders: generadas };
    }
  }

  if (partes[0] === 'work-orders') {
    if (metodo === 'GET' && partes.length === 1) {
      const estado = q.get('status');
      const tipo = q.get('type');
      const soloMias = q.get('mine') === 'true';
      return pagina(ORDENES.filter((o) => (!estado || o.status === estado) && (!tipo || o.type === tipo)
        && (!soloMias || o.assignedToId === sesion.id))
        .sort((x, y) => x.dueDate.localeCompare(y.dueDate)), q);
    }
    if (metodo === 'GET') {
      return detalle(id);
    }
    if (metodo === 'POST' && partes.length === 1) {
      nuevaOrden(cuerpo as never);
      return detalle(siguienteId - 1);
    }
    if (metodo === 'PATCH') {
      return transicion(id, accion, cuerpo);
    }
  }
  return undefined;
}

function nuevaOrden(datos: {
  assetId: number; type: WorkOrderSummary['type']; priority: WorkOrderSummary['priority'];
  title: string; dueDate: string; assignedToId: number | null;
}): WorkOrderSummary {
  const activo = ACTIVOS.find((a) => a.id === Number(datos.assetId))!;
  const tecnico = USUARIOS.find((u) => u.id === Number(datos.assignedToId));
  const nueva: WorkOrderSummary = {
    id: siguienteId, code: `OT-2026-${String(siguienteId).padStart(5, '0')}`, title: datos.title,
    type: datos.type, priority: datos.priority, status: tecnico ? 'ASIGNADA' : 'ABIERTA',
    assetId: activo.id, assetCode: activo.code, assetName: activo.name,
    assignedToId: tecnico?.id ?? null, assignedToName: tecnico?.fullName ?? null,
    dueDate: datos.dueDate, overdue: false, createdAt: new Date().toISOString().slice(0, 19), closedAt: null,
  };
  siguienteId++;
  ORDENES.unshift(nueva);
  DETALLES.set(nueva.id, { summary: nueva, description: null, createdByName: sesion.fullName,
    preventivePlanId: null, failureAt: nueva.createdAt, startedAt: null, laborHours: null,
    resolutionNotes: null, parts: [], partsCost: 0 });
  return nueva;
}

function transicion(id: number, accion: string, cuerpo: Record<string, never> | null): WorkOrderDetail {
  const orden = resumen(id);
  const d = DETALLES.get(id)!;
  const activo = ACTIVOS.find((a) => a.id === orden.assetId)!;

  if (accion === 'assign') {
    const tecnico = USUARIOS.find((u) => u.id === Number(cuerpo?.['technicianId']))!;
    orden.assignedToId = tecnico.id;
    orden.assignedToName = tecnico.fullName;
    orden.status = 'ASIGNADA';
  } else if (accion === 'start') {
    if (orden.status !== 'ASIGNADA') {
      throw error(409, `No se puede iniciar una orden en estado ${orden.status}`);
    }
    orden.status = 'EN_PROCESO';
    d.startedAt = new Date().toISOString().slice(0, 19);
    activo.status = 'EN_MANTENIMIENTO';
  } else if (accion === 'close') {
    if (orden.status !== 'EN_PROCESO') {
      throw error(409, `No se puede cerrar una orden en estado ${orden.status}`);
    }
    const lineas = (cuerpo?.['parts'] as unknown as { sparePartId: number; quantity: number }[]) ?? [];
    for (const linea of lineas) {
      const parte = REFACCIONES.find((r) => r.id === Number(linea.sparePartId))!;
      if (linea.quantity > parte.stock) {
        throw error(422, `Existencia insuficiente de ${parte.sku}: disponible ${parte.stock}, `
          + `solicitado ${linea.quantity}`);
      }
    }
    d.parts = lineas.map((linea) => {
      const parte = REFACCIONES.find((r) => r.id === Number(linea.sparePartId))!;
      parte.stock -= linea.quantity;
      parte.belowReorderPoint = parte.stock <= parte.reorderPoint;
      return { sparePartId: parte.id, sku: parte.sku, name: parte.name, quantity: linea.quantity,
        unitCost: parte.unitCost, subtotal: parte.unitCost * linea.quantity };
    });
    d.partsCost = d.parts.reduce((total, linea) => total + linea.subtotal, 0);
    d.laborHours = Number(cuerpo?.['laborHours'] ?? 0);
    d.resolutionNotes = String(cuerpo?.['resolutionNotes'] ?? '');
    orden.status = 'CERRADA';
    orden.closedAt = new Date().toISOString().slice(0, 19);
    orden.overdue = false;
    activo.status = 'OPERATIVO';
  } else if (accion === 'cancel') {
    orden.status = 'CANCELADA';
    d.resolutionNotes = 'Cancelada: ' + String(cuerpo?.['reason'] ?? '');
    orden.overdue = false;
  }
  return detalle(id);
}
