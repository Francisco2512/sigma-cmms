import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth';
import { WorkOrderDetail, WorkOrderStatus } from '../../core/models';
import { SparePartsService } from '../spare-parts/spare-parts.service';
import { WorkOrderDetailComponent } from './work-order-detail.component';
import { WorkOrdersService } from './work-orders.service';

function detail(status: WorkOrderStatus, assignedToId: number | null): WorkOrderDetail {
  return {
    summary: {
      id: 1, code: 'OT-2026-00001', title: 'Fuga de aceite', type: 'CORRECTIVA', priority: 'ALTA', status,
      assetId: 3, assetCode: 'HID-002', assetName: 'Unidad hidráulica', assignedToId,
      assignedToName: assignedToId ? 'Luis Hernández' : null, dueDate: '2026-09-22', overdue: false,
      createdAt: '2026-09-21T10:00:00', closedAt: null,
    },
    description: null, createdByName: 'Martha Ruiz', preventivePlanId: null, failureAt: null, startedAt: null,
    laborHours: null, resolutionNotes: null, parts: [], partsCost: 0,
  };
}

describe('WorkOrderDetailComponent', () => {
  let fixture: ComponentFixture<WorkOrderDetailComponent>;
  let orders: jasmine.SpyObj<WorkOrdersService>;

  async function render(order: WorkOrderDetail): Promise<HTMLElement> {
    orders.get.and.returnValue(of(order));
    fixture = TestBed.createComponent(WorkOrderDetailComponent);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    orders = jasmine.createSpyObj<WorkOrdersService>('WorkOrdersService', ['get', 'start', 'technicians']);
    const parts = jasmine.createSpyObj<SparePartsService>('SparePartsService', ['search']);
    parts.search.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }));
    const technician = { id: 4, username: 'lhernandez', fullName: 'Luis Hernández', role: 'TECNICO' as const };
    const auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole'], { user: signal(technician) });
    auth.hasAnyRole.and.returnValue(false);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: WorkOrdersService, useValue: orders },
        { provide: SparePartsService, useValue: parts },
        { provide: AuthService, useValue: auth },
      ],
    });
  });

  it('should_offerStart_when_technicianOwnsAssignedOrder', async () => {
    // Act
    const element = await render(detail('ASIGNADA', 4));

    // Assert
    expect(element.querySelector('[data-testid="start-order"]')).not.toBeNull();
  });

  it('should_hideStart_when_orderBelongsToAnotherTechnician', async () => {
    // Act
    const element = await render(detail('ASIGNADA', 9));

    // Assert
    expect(element.querySelector('[data-testid="start-order"]')).toBeNull();
  });

  it('should_callStartService_when_startIsClicked', async () => {
    // Arrange
    orders.start.and.returnValue(of(detail('EN_PROCESO', 4)));
    const element = await render(detail('ASIGNADA', 4));

    // Act
    (element.querySelector('[data-testid="start-order"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    // Assert
    expect(orders.start).toHaveBeenCalledWith(1);
    expect(element.querySelector('[data-testid="close-form"]')).not.toBeNull();
  });
});
