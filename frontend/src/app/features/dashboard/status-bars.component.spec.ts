import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBarsComponent } from './status-bars.component';

describe('StatusBarsComponent', () => {
  let fixture: ComponentFixture<StatusBarsComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(StatusBarsComponent);
  });

  it('should_renderAllStatusesWithZeroDefault_when_someAreMissing', () => {
    // Arrange
    fixture.componentRef.setInput('counts', { ABIERTA: 3, CERRADA: 12 });

    // Act
    fixture.detectChanges();

    // Assert
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('li').length).toBe(5);
    expect(element.querySelector('[data-testid="bar-CERRADA"] .total')?.textContent).toBe('12');
    expect(element.querySelector('[data-testid="bar-EN_PROCESO"] .total')?.textContent).toBe('0');
  });
});
