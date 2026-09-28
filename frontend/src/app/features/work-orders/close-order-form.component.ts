import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SparePart, WorkOrderClose } from '../../core/models';

type PartRow = FormGroup<{ sparePartId: FormControl<number>; quantity: FormControl<number> }>;

/** Formulario de cierre: horas, solución y refacciones consumidas. */
@Component({
  selector: 'app-close-order-form',
  imports: [ReactiveFormsModule],
  templateUrl: './close-order-form.component.html',
  styleUrl: './close-order-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CloseOrderFormComponent {
  readonly catalog = input.required<SparePart[]>();
  readonly busy = input(false);
  readonly closeRequested = output<WorkOrderClose>();

  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly form = this.fb.group({
    laborHours: [1, [Validators.required, Validators.min(0.25), Validators.max(999.99)]],
    resolutionNotes: ['', [Validators.required, Validators.maxLength(1000)]],
    parts: this.fb.array<PartRow>([]),
  });

  protected get parts(): FormArray<PartRow> {
    return this.form.controls.parts;
  }

  protected addPart(): void {
    this.parts.push(this.partRow());
  }

  protected removePart(index: number): void {
    this.parts.removeAt(index);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.closeRequested.emit({
      laborHours: value.laborHours,
      resolutionNotes: value.resolutionNotes,
      parts: value.parts.map((row) => ({ sparePartId: row.sparePartId, quantity: row.quantity })),
    });
  }

  private partRow(): PartRow {
    return this.fb.group({
      sparePartId: [0, [Validators.required, Validators.min(1)]],
      quantity: [1, [Validators.required, Validators.min(1), Validators.max(1000)]],
    });
  }
}
