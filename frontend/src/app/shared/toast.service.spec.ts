import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    service = TestBed.inject(ToastService);
  });

  it('should_showToast_when_successIsCalled', fakeAsync(() => {
    // Act
    service.success('Orden cerrada');

    // Assert
    expect(service.toasts().map((toast) => toast.text)).toEqual(['Orden cerrada']);
    tick(5000);
  }));

  it('should_removeToast_when_durationElapses', fakeAsync(() => {
    // Arrange
    service.error('Sin conexión');

    // Act
    tick(5000);

    // Assert
    expect(service.toasts()).toEqual([]);
  }));

  it('should_removeOnlyThatToast_when_dismissed', fakeAsync(() => {
    // Arrange
    service.success('uno');
    service.success('dos');

    // Act
    service.dismiss(service.toasts()[0].id);

    // Assert
    expect(service.toasts().map((toast) => toast.text)).toEqual(['dos']);
    tick(5000);
  }));
});
