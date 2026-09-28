import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, STOCK_ROLES } from '../../core/auth';
import { problemMessage } from '../../core/http/problem';
import { PageResponse, SparePart } from '../../core/models';
import { BadgeComponent, ToastService } from '../../shared';
import { SparePartsService } from './spare-parts.service';

/** Inventario de refacciones con alerta de reorden y registro de entradas (RF-06). */
@Component({
  selector: 'app-spare-part-list',
  imports: [BadgeComponent, CurrencyPipe, FormsModule],
  templateUrl: './spare-part-list.component.html',
  styles: '.restock { display: flex; gap: 6px; justify-content: flex-end; } .restock .input { width: 80px; min-height: 34px; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SparePartListComponent implements OnInit {
  private readonly service = inject(SparePartsService);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);

  protected readonly canRestock = inject(AuthService).hasAnyRole(STOCK_ROLES);
  protected readonly search = signal('');
  protected readonly onlyAlerts = signal(false);
  protected readonly result = signal<PageResponse<SparePart> | null>(null);
  protected readonly quantities = signal<Record<number, number>>({});

  ngOnInit(): void {
    this.onlyAlerts.set(this.route.snapshot.queryParamMap.get('alerts') === 'true');
    void this.load(0);
  }

  protected async load(page: number): Promise<void> {
    try {
      this.result.set(await firstValueFrom(this.service.search(this.search(), this.onlyAlerts(), page)));
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }

  protected setQuantity(partId: number, value: number): void {
    this.quantities.update((current) => ({ ...current, [partId]: value }));
  }

  protected async restock(part: SparePart): Promise<void> {
    const quantity = this.quantities()[part.id] ?? 0;
    if (quantity < 1) {
      return;
    }
    try {
      const updated = await firstValueFrom(this.service.restock(part.id, quantity));
      this.result.update((page) =>
        page ? { ...page, content: page.content.map((item) => (item.id === updated.id ? updated : item)) } : page,
      );
      this.setQuantity(part.id, 0);
      this.toast.success(`Entrada registrada: ${part.sku} ahora tiene ${updated.stock} ${updated.unit}`);
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }
}
