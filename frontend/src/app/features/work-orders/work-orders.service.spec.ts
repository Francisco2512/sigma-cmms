import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { WorkOrdersService } from './work-orders.service';

describe('WorkOrdersService', () => {
  let service: WorkOrdersService;
  let http: HttpTestingController;
  const wrap = (data: unknown) => ({ success: true, data, message: null, timestamp: '' });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(WorkOrdersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should_omitEmptyFilters_when_listingOrders', () => {
    // Act
    service.list({ status: '', type: 'CORRECTIVA', mine: true, page: 0 }).subscribe();

    // Assert
    const request = http.expectOne((req) => req.url === '/api/v1/work-orders');
    expect(request.request.params.has('status')).toBeFalse();
    expect(request.request.params.get('type')).toBe('CORRECTIVA');
    expect(request.request.params.get('mine')).toBe('true');
    request.flush(wrap({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 15 }));
  });

  it('should_patchCloseEndpoint_when_closingOrder', () => {
    // Arrange
    const body = { laborHours: 2, resolutionNotes: 'Cambio de sello', parts: [{ sparePartId: 5, quantity: 1 }] };

    // Act
    service.close(7, body).subscribe();

    // Assert
    const request = http.expectOne('/api/v1/work-orders/7/close');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(body);
    request.flush(wrap({}));
  });

  it('should_requestTechniciansOnce_when_calledTwice', () => {
    // Act
    service.technicians().subscribe();
    service.technicians().subscribe();

    // Assert
    http.expectOne((req) => req.url === '/api/v1/users').flush(wrap([]));
  });

  it('should_sendReason_when_cancellingOrder', () => {
    // Act
    service.cancel(3, 'Reporte duplicado').subscribe();

    // Assert
    const request = http.expectOne('/api/v1/work-orders/3/cancel');
    expect(request.request.body).toEqual({ reason: 'Reporte duplicado' });
    request.flush(wrap({}));
  });
});
