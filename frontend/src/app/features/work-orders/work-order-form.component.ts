import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, PLANNER_ROLES } from '../../core/auth';
import { problemMessage } from '../../core/http/problem';
import { Asset, Level, UserSummary, WorkOrderType } from '../../core/models';
import { ToastService } from '../../shared';
import { AssetsService } from '../assets/assets.service';
import { WorkOrdersService } from './work-orders.service';

/** Alta de una orden. Un técnico solo reporta fallas, sin asignarlas. */
@Component({
  selector: 'app-work-order-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './work-order-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkOrderFormComponent implements OnInit {
  private readonly service = inject(WorkOrdersService);
  private readonly assetsService = inject(AssetsService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly isPlanner = inject(AuthService).hasAnyRole(PLANNER_ROLES);
  protected readonly assets = signal<Asset[]>([]);
  protected readonly technicians = signal<UserSummary[]>([]);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    assetId: [0, [Validators.required, Validators.min(1)]],
    type: ['CORRECTIVA' as WorkOrderType, Validators.required],
    priority: ['MEDIA' as Level, Validators.required],
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', Validators.maxLength(1000)],
    dueDate: [new Date().toISOString().slice(0, 10), Validators.required],
    assignedToId: [0],
  });

  async ngOnInit(): Promise<void> {
    if (!this.isPlanner) {
      this.form.controls.type.disable();
    }
    try {
      const page = await firstValueFrom(this.assetsService.search('', '', 0, 100));
      this.assets.set(page.content);
      if (this.isPlanner) {
        this.technicians.set(await firstValueFrom(this.service.technicians()));
      }
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    try {
      const value = this.form.getRawValue();
      const created = await firstValueFrom(
        this.service.create({ ...value, assignedToId: value.assignedToId || null }),
      );
      this.toast.success(`Orden ${created.summary.code} creada`);
      await this.router.navigate(['/work-orders', created.summary.id]);
    } catch (err) {
      this.toast.error(problemMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
