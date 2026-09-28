import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService, PLANNER_ROLES, SUPERVISOR_ROLES } from '../../core/auth';
import { problemMessage } from '../../core/http/problem';
import { SparePart, UserSummary, WorkOrderClose, WorkOrderDetail } from '../../core/models';
import { BadgeComponent, LEVEL_LABELS, LEVEL_TONES, STATUS_LABELS, STATUS_TONES, ToastService } from '../../shared';
import { SparePartsService } from '../spare-parts/spare-parts.service';
import { CloseOrderFormComponent } from './close-order-form.component';
import { WorkOrdersService } from './work-orders.service';

/** Detalle de una orden con las acciones que permite su estado y el rol del usuario. */
@Component({
  selector: 'app-work-order-detail',
  imports: [BadgeComponent, CloseOrderFormComponent, CurrencyPipe, DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './work-order-detail.component.html',
  styleUrl: './work-order-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderDetailComponent implements OnInit {
  /** Id de la ruta (enlazado por el router). */
  readonly id = input.required<string>();

  private readonly service = inject(WorkOrdersService);
  private readonly partsService = inject(SparePartsService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly order = signal<WorkOrderDetail | null>(null);
  protected readonly technicians = signal<UserSummary[]>([]);
  protected readonly catalog = signal<SparePart[]>([]);
  protected readonly busy = signal(false);
  protected readonly technicianId = signal(0);
  protected readonly cancelReason = signal('');

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusTones = STATUS_TONES;
  protected readonly levelLabels = LEVEL_LABELS;
  protected readonly levelTones = LEVEL_TONES;

  private readonly status = computed(() => this.order()?.summary.status);
  private readonly isMine = computed(() => this.order()?.summary.assignedToId === this.auth.user()?.id);
  private readonly isSupervisor = this.auth.hasAnyRole(SUPERVISOR_ROLES);
  private readonly isPlanner = this.auth.hasAnyRole(PLANNER_ROLES);

  protected readonly canAssign = computed(() => this.isPlanner && ['ABIERTA', 'ASIGNADA'].includes(this.status() ?? ''));
  protected readonly canStart = computed(() => this.status() === 'ASIGNADA' && (this.isSupervisor || this.isMine()));
  protected readonly canClose = computed(() => this.status() === 'EN_PROCESO' && (this.isSupervisor || this.isMine()));
  protected readonly canCancel = computed(
    () => this.isSupervisor && ['ABIERTA', 'ASIGNADA'].includes(this.status() ?? ''),
  );

  async ngOnInit(): Promise<void> {
    await this.load();
    try {
      if (this.canAssign()) {
        this.technicians.set(await firstValueFrom(this.service.technicians()));
      }
      if (this.canClose()) {
        await this.loadCatalog();
      }
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }

  protected assign(): void {
    void this.run(this.service.assign(this.orderId, this.technicianId()), 'Orden asignada');
  }

  protected async start(): Promise<void> {
    await this.run(this.service.start(this.orderId), 'Orden iniciada; el activo pasó a mantenimiento');
    if (this.canClose()) {
      try {
        await this.loadCatalog();
      } catch (err) {
        this.toast.error(problemMessage(err));
      }
    }
  }

  protected close(request: WorkOrderClose): void {
    void this.run(this.service.close(this.orderId, request), 'Orden cerrada y refacciones descontadas');
  }

  protected cancel(): void {
    void this.run(this.service.cancel(this.orderId, this.cancelReason()), 'Orden cancelada');
  }

  private get orderId(): number {
    return Number(this.id());
  }

  private async load(): Promise<void> {
    try {
      this.order.set(await firstValueFrom(this.service.get(this.orderId)));
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }

  private async loadCatalog(): Promise<void> {
    const page = await firstValueFrom(this.partsService.search('', false, 0, 100));
    this.catalog.set(page.content);
  }

  private async run(action: Observable<WorkOrderDetail>, message: string): Promise<void> {
    this.busy.set(true);
    try {
      this.order.set(await firstValueFrom(action));
      this.toast.success(message);
    } catch (err) {
      this.toast.error(problemMessage(err));
    } finally {
      this.busy.set(false);
    }
  }
}
