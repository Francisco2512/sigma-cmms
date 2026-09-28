import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let auth: jasmine.SpyObj<AuthService>;
  let element: HTMLElement;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'homeRoute']);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  function type(testId: string, value: string): void {
    const input = element.querySelector(`[data-testid="${testId}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('should_notCallLogin_when_fieldsAreEmpty', () => {
    // Act
    (element.querySelector('[data-testid="login-submit"]') as HTMLButtonElement).click();

    // Assert
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('should_showBackendMessage_when_credentialsAreRejected', async () => {
    // Arrange
    auth.login.and.rejectWith(new HttpErrorResponse({ status: 401, error: { detail: 'Usuario o contrasena incorrectos' } }));
    type('login-username', 'lhernandez');
    type('login-password', 'mala');

    // Act
    (element.querySelector('[data-testid="login-submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    // Assert
    expect(auth.login).toHaveBeenCalledWith('lhernandez', 'mala');
    expect(element.querySelector('[data-testid="login-error"]')?.textContent).toContain('incorrectos');
  });
});
