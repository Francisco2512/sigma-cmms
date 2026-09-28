import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, REPORTER_ROLES } from '../../core/auth';
import { problemMessage } from '../../core/http/problem';
import { PageResponse, STATUS_LABELS, WorkOrderStatus, WorkOrderSummary, WorkOrderType } from '../../core/models';
import { IconComponent, ToastService } from '../../shared';
import { WorkOrderTableComponent } from './work-order-table.component';
import { WorkOrdersService } from './work-orders.service';

/** Listado de órdenes con filtros; los técnicos ven primero las suyas. */
@Component({
  selector: 'app-work-order-list',
  imports: [FormsModule, RouterLink, IconComponent, WorkOrderTableComponent],
  templateUrl: './work-order-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderListComponent implements OnInit {
  private readonly service = inject(WorkOrdersService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly statuses = Object.entries(STATUS_LABELS) as [WorkOrderStatus, string][];
  protected readonly canCreate = this.auth.hasAnyRole(REPORTER_ROLES);
  protected readonly status = signal<WorkOrderStatus | ''>('');
  protected readonly type = signal<WorkOrderType | ''>('');
  protected readonly mine = signal(this.auth.user()?.role === 'TECNICO');
  protected readonly page = signal(0);
  protected readonly result = signal<PageResponse<WorkOrderSummary> | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    void this.load();
  }

  protected applyFilters(): void {
    this.page.set(0);
    void this.load();
  }

  protected goTo(page: number): void {
    this.page.set(page);
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      const filters = { status: this.status(), type: this.type(), mine: this.mine(), page: this.page() };
      this.result.set(await firstValueFrom(this.service.list(filters)));
    } catch (err) {
      this.toast.error(problemMessage(err));
    } finally {
      this.loading.set(false);
    }
  }
}
