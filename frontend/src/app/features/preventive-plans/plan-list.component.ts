import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { problemMessage } from '../../core/http/problem';
import { Asset, GenerationResult, PlanCreate, PreventivePlan } from '../../core/models';
import { BadgeComponent, IconComponent, LEVEL_LABELS, LEVEL_TONES, ToastService } from '../../shared';
import { AssetsService } from '../assets/assets.service';
import { PlanFormComponent } from './plan-form.component';
import { PlansService } from './plans.service';

/** Planes preventivos y generación de sus órdenes (RF-03). */
@Component({
  selector: 'app-plan-list',
  imports: [BadgeComponent, DatePipe, IconComponent, PlanFormComponent, RouterLink],
  templateUrl: './plan-list.component.html',
  styles: '.result { background: var(--success-soft); border-color: #bfe3cc; } .result ul { margin: 8px 0 0; padding-left: 18px; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanListComponent implements OnInit {
  private readonly service = inject(PlansService);
  private readonly assetsService = inject(AssetsService);
  private readonly toast = inject(ToastService);

  protected readonly levelLabels = LEVEL_LABELS;
  protected readonly levelTones = LEVEL_TONES;
  protected readonly plans = signal<PreventivePlan[]>([]);
  protected readonly assets = signal<Asset[]>([]);
  protected readonly showForm = signal(false);
  protected readonly busy = signal(false);
  protected readonly lastRun = signal<GenerationResult | null>(null);

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected async openForm(): Promise<void> {
    this.showForm.set(true);
    if (this.assets().length === 0) {
      const page = await firstValueFrom(this.assetsService.search('', '', 0, 100));
      this.assets.set(page.content);
    }
  }

  protected async create(body: PlanCreate): Promise<void> {
    await this.act(async () => {
      const plan = await firstValueFrom(this.service.create(body));
      this.toast.success(`Plan "${plan.name}" creado`);
      this.showForm.set(false);
    });
  }

  protected async toggle(plan: PreventivePlan): Promise<void> {
    await this.act(async () => {
      await firstValueFrom(this.service.changeStatus(plan.id, plan.status === 'ACTIVO' ? 'PAUSADO' : 'ACTIVO'));
    });
  }

  protected async generate(): Promise<void> {
    await this.act(async () => {
      const result = await firstValueFrom(this.service.generate());
      this.lastRun.set(result);
      this.toast.success(`${result.generated} órdenes preventivas generadas`);
    });
  }

  private async act(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    try {
      await action();
      await this.load();
    } catch (err) {
      this.toast.error(problemMessage(err));
    } finally {
      this.busy.set(false);
    }
  }

  private async load(): Promise<void> {
    try {
      this.plans.set(await firstValueFrom(this.service.list()));
    } catch (err) {
      this.toast.error(problemMessage(err));
    }
  }
}
