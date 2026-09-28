import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { problemMessage } from '../../core/http/problem';
import { Kpis } from '../../core/models';
import { KpiCardComponent, ToastService } from '../../shared';
import { DashboardService } from './dashboard.service';
import { StatusBarsComponent } from './status-bars.component';
import { TopAssetsComponent } from './top-assets.component';

/** Tablero de indicadores del área de mantenimiento (RF-08). */
@Component({
  selector: 'app-dashboard',
  imports: [KpiCardComponent, TopAssetsComponent, StatusBarsComponent, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly service = inject(DashboardService);
  private readonly toast = inject(ToastService);

  protected readonly periods = [30, 90, 180];
  protected readonly days = signal(90);
  protected readonly kpis = signal<Kpis | null>(null);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    void this.load();
  }

  protected selectPeriod(days: number): void {
    this.days.set(days);
    void this.load();
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.kpis.set(await firstValueFrom(this.service.getKpis(this.days())));
    } catch (err) {
      this.toast.error(problemMessage(err));
    } finally {
      this.loading.set(false);
    }
  }
}
