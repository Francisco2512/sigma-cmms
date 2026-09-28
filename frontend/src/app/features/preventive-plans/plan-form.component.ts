import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Asset, Level, PlanCreate } from '../../core/models';

/** Formulario de alta de un plan preventivo. */
@Component({
  selector: 'app-plan-form',
  imports: [ReactiveFormsModule],
  templateUrl: './plan-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanFormComponent {
  readonly assets = input.required<Asset[]>();
  readonly busy = input(false);
  readonly saved = output<PlanCreate>();
  readonly cancelled = output<void>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    assetId: [0, [Validators.required, Validators.min(1)]],
    frequencyDays: [30, [Validators.required, Validators.min(1), Validators.max(730)]],
    nextDueDate: [new Date().toISOString().slice(0, 10), Validators.required],
    priority: ['MEDIA' as Level, Validators.required],
    taskDescription: ['', [Validators.required, Validators.maxLength(1000)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.emit(this.form.getRawValue());
  }
}
