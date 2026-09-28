import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { problemMessage } from '../../core/http/problem';
import { Asset, AssetStatus, PageResponse } from '../../core/models';
import { ASSET_STATUS, BadgeComponent, LEVEL_LABELS, LEVEL_TONES, ToastService } from '../../shared';
import { AssetsService } from './assets.service';

/** Catálogo de activos con búsqueda. */
@Component({
  selector: 'app-asset-list',
  imports: [BadgeComponent, FormsModule, RouterLink],
  templateUrl: './asset-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetListComponent implements OnInit {
  private readonly service = inject(AssetsService);
  private readonly toast = inject(ToastService);

  protected readonly assetStatus = ASSET_STATUS;
  protected readonly levelLabels = LEVEL_LABELS;
  protected readonly levelTones = LEVEL_TONES;
  protected readonly search = signal('');
  protected readonly status = signal<AssetStatus | ''>('');
  protected readonly result = signal<PageResponse<Asset> | null>(null);

  ngOnInit(): void {
    void this.load(0);
  }

  protected async load(page: number): Promise<void> {
    try {
      this.result.set(await firstValueFrom(this.service.search(this.search(), this.status(), page)));
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }
}
