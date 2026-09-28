import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Tarjeta de un indicador numérico. Un valor nulo se muestra como "Sin datos". */
@Component({
  selector: 'app-kpi-card',
  imports: [DecimalPipe],
  templateUrl: './kpi-card.component.html',
  styleUrl: './kpi-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input<number | null>(null);
  readonly unit = input<string>('');
  readonly hint = input<string>('');
  readonly decimals = input<number>(1);
  readonly tone = input<'default' | 'good' | 'alert'>('default');

  protected readonly format = computed(() => `1.${this.decimals()}-${this.decimals()}`);
}
