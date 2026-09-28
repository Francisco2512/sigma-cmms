import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AssetKpi } from '../../core/models';

/** Activos con más fallas en el periodo. */
@Component({
  selector: 'app-top-assets',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './top-assets.component.html',
  styles: `.avail { display: flex; align-items: center; gap: 8px; justify-content: flex-end; }
    .track { width: 70px; height: 6px; border-radius: 3px; background: var(--neutral-soft); overflow: hidden; }
    .fill { display: block; height: 100%; background: var(--success-strong); }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopAssetsComponent {
  readonly assets = input.required<AssetKpi[]>();
}
