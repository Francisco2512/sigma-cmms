import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const PATHS = {
  dashboard: 'M3 3h8v8H3zM13 3h8v5h-8zM13 10h8v11h-8zM3 13h8v8H3z',
  orders: 'M9 3h6v3H9zM7 5H5v16h14V5h-2M8 12h8M8 16h5',
  assets: 'M3 21V10l6 4v-4l6 4V5h6v16zM7 17h2M12 17h2M17 17h2',
  parts: 'M21 8l-9-5-9 5 9 5zM3 8v8l9 5 9-5V8M12 13v8',
  plans: 'M4 5h16v16H4zM4 10h16M9 3v4M15 3v4M8 14h3',
  logout: 'M9 21H5V3h4M16 17l5-5-5-5M21 12H9',
  menu: 'M3 6h18M3 12h18M3 18h18',
  plus: 'M12 5v14M5 12h14',
  alert: 'M12 3l10 18H2zM12 10v5M12 18v.5',
} as const;

/** Nombre de un ícono disponible. */
export type IconName = keyof typeof PATHS;

/** Ícono SVG de trazo, hereda el color del texto. */
@Component({
  selector: 'app-icon',
  template: `<svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="path()" /></svg>`,
  styles: ':host { display: inline-flex; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input<number>(20);
  protected readonly path = computed(() => PATHS[this.name()]);
}
