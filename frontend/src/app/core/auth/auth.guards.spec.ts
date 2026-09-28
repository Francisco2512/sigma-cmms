import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard, roleGuard } from './auth.guards';
import { AuthService } from './auth.service';
import { PLANNER_ROLES } from './roles';

describe('auth guards', () => {
  let auth: jasmine.SpyObj<AuthService>;
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'hasAnyRole', 'homeRoute']);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  const run = (guard: typeof authGuard) => TestBed.runInInjectionContext(() => guard(route, state));
  const url = (result: unknown) => TestBed.inject(Router).serializeUrl(result as UrlTree);

  it('should_redirectToLogin_when_thereIsNoSession', () => {
    // Arrange
    auth.isAuthenticated.and.returnValue(false);

    // Act + Assert
    expect(url(run(authGuard))).toBe('/login');
  });

  it('should_allowAccess_when_roleIsPermitted', () => {
    // Arrange
    auth.isAuthenticated.and.returnValue(true);
    auth.hasAnyRole.and.returnValue(true);

    // Act + Assert
    expect(run(roleGuard(PLANNER_ROLES))).toBeTrue();
  });

  it('should_redirectToHome_when_roleIsNotPermitted', () => {
    // Arrange
    auth.isAuthenticated.and.returnValue(true);
    auth.hasAnyRole.and.returnValue(false);
    auth.homeRoute.and.returnValue('/work-orders');

    // Act + Assert
    expect(url(run(roleGuard(PLANNER_ROLES)))).toBe('/work-orders');
  });
});
