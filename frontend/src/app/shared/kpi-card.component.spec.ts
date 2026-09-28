import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KpiCardComponent } from './kpi-card.component';

describe('KpiCardComponent', () => {
  let fixture: ComponentFixture<KpiCardComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(KpiCardComponent);
    fixture.componentRef.setInput('label', 'MTTR');
  });

  const valueText = () =>
    (fixture.nativeElement as HTMLElement).querySelector('[data-testid="kpi-value"]')?.textContent?.trim();

  it('should_showValueWithUnit_when_valueIsPresent', () => {
    // Arrange
    fixture.componentRef.setInput('value', 4.25);
    fixture.componentRef.setInput('unit', 'h');

    // Act
    fixture.detectChanges();

    // Assert
    expect(valueText()).toBe('4.3h');
  });

  it('should_showNoData_when_valueIsNull', () => {
    // Act
    fixture.detectChanges();

    // Assert
    expect(valueText()).toBe('Sin datos');
  });
});
