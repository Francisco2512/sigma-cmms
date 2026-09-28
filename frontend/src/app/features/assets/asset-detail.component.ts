import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { problemMessage } from '../../core/http/problem';
import { Asset, PageResponse, WorkOrderSummary } from '../../core/models';
import { ASSET_STATUS, BadgeComponent, LEVEL_LABELS, LEVEL_TONES, ToastService } from '../../shared';
import { WorkOrderTableComponent } from '../work-orders/work-order-table.component';
import { AssetsService } from './assets.service';

/** Ficha técnica del activo con su historial de intervenciones (RF-01). */
@Component({
  selector: 'app-asset-detail',
  imports: [BadgeComponent, DatePipe, RouterLink, WorkOrderTableComponent],
  templateUrl: './asset-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetDetailComponent implements OnInit {
  /** Id de la ruta (enlazado por el router). */
  readonly id = input.required<string>();

  private readonly service = inject(AssetsService);
  private readonly toast = inject(ToastService);

  protected readonly assetStatus = ASSET_STATUS;
  protected readonly levelLabels = LEVEL_LABELS;
  protected readonly levelTones = LEVEL_TONES;
  protected readonly asset = signal<Asset | null>(null);
  protected readonly history = signal<PageResponse<WorkOrderSummary> | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      this.asset.set(await firstValueFrom(this.service.get(Number(this.id()))));
      await this.loadHistory(0);
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }

  protected async loadHistory(page: number): Promise<void> {
    this.history.set(await firstValueFrom(this.service.history(Number(this.id()), page)));
  }
}
