import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { WorkOrderSummary } from '../../core/models';
import { BadgeComponent, LEVEL_LABELS, LEVEL_TONES, STATUS_LABELS, STATUS_TONES } from '../../shared';

/** Tabla de órdenes; cada fila enlaza al detalle. */
@Component({
  selector: 'app-work-order-table',
  imports: [BadgeComponent, DatePipe, RouterLink],
  templateUrl: './work-order-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderTableComponent {
  readonly orders = input.required<WorkOrderSummary[]>();
  readonly showAsset = input(true);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusTones = STATUS_TONES;
  protected readonly levelLabels = LEVEL_LABELS;
  protected readonly levelTones = LEVEL_TONES;
}
