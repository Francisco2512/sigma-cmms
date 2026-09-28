import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PlansService } from './plans.service';

describe('PlansService', () => {
  let service: PlansService;
  let http: HttpTestingController;
  const wrap = (data: unknown) => ({ success: true, data, message: null, timestamp: '' });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PlansService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should_unwrapGenerationResult_when_generatingOrders', () => {
    // Arrange
    let generated = -1;

    // Act
    service.generate().subscribe((result) => (generated = result.generated));
    http.expectOne('/api/v1/preventive-plans/generate').flush(wrap({ generated: 2, skippedCycles: 0, orders: [] }));

    // Assert
    expect(generated).toBe(2);
  });

  it('should_patchStatus_when_pausingPlan', () => {
    // Act
    service.changeStatus(4, 'PAUSADO').subscribe();

    // Assert
    const request = http.expectOne('/api/v1/preventive-plans/4/status');
    expect(request.request.body).toEqual({ status: 'PAUSADO' });
    request.flush(wrap({}));
  });
});
