import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Tono visual de una insignia. */
export type BadgeTone = 'neutral' | 'info' | 'progress' | 'success' | 'warning' | 'danger';

/** Insignia de estado o prioridad. */
@Component({
  selector: 'app-badge',
  template: '<span class="badge" [class]="\'badge badge-\' + tone()">{{ label() }}</span>',
  styleUrl: './badge.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BadgeComponent {
  readonly label = input.required<string>();
  readonly tone = input<BadgeTone>('neutral');
}
