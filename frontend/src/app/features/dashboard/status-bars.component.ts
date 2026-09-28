import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { WorkOrderStatus } from '../../core/models';
import { BadgeTone, STATUS_LABELS, STATUS_TONES } from '../../shared';

const ORDER: WorkOrderStatus[] = ['ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'CERRADA', 'CANCELADA'];

/** Barras horizontales con el número de órdenes por estatus. */
@Component({
  selector: 'app-status-bars',
  templateUrl: './status-bars.component.html',
  styleUrl: './status-bars.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBarsComponent {
  readonly counts = input.required<Record<string, number>>();

  protected readonly rows = computed(() => {
    const counts = this.counts();
    const max = Math.max(1, ...ORDER.map((status) => counts[status] ?? 0));
    return ORDER.map((status) => ({
      status,
      label: STATUS_LABELS[status],
      tone: STATUS_TONES[status] as BadgeTone,
      total: counts[status] ?? 0,
      pct: ((counts[status] ?? 0) / max) * 100,
    }));
  });
}
