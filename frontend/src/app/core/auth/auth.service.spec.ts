import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  const loginResponse = (role: string) => ({
    success: true,
    message: null,
    timestamp: '',
    data: {
      token: 'jwt',
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      user: { id: 4, username: 'lhernandez', fullName: 'Luis Hernández', role },
    },
  });

  beforeEach(() => {
    sessionStorage.clear();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: Router, useValue: router }],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should_storeSessionAndExposeUser_when_loginSucceeds', async () => {
    // Arrange
    const pending = service.login('lhernandez', 'secreta');

    // Act
    http.expectOne('/api/v1/auth/login').flush(loginResponse('TECNICO'));
    await pending;

    // Assert
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token).toBe('jwt');
    expect(sessionStorage.getItem('sigma.session')).toContain('lhernandez');
  });

  it('should_sendTechnicianToWorkOrders_when_computingHomeRoute', async () => {
    // Arrange
    const pending = service.login('lhernandez', 'secreta');
    http.expectOne('/api/v1/auth/login').flush(loginResponse('TECNICO'));
    await pending;

    // Act + Assert
    expect(service.homeRoute()).toBe('/work-orders');
  });

  it('should_sendPlannerToDashboard_when_computingHomeRoute', async () => {
    // Arrange
    const pending = service.login('jcastillo', 'secreta');
    http.expectOne('/api/v1/auth/login').flush(loginResponse('PLANIFICADOR'));
    await pending;

    // Act + Assert
    expect(service.homeRoute()).toBe('/dashboard');
  });

  it('should_clearSessionAndRedirect_when_loggingOut', async () => {
    // Arrange
    const pending = service.login('lhernandez', 'secreta');
    http.expectOne('/api/v1/auth/login').flush(loginResponse('TECNICO'));
    await pending;

    // Act
    service.logout();

    // Assert
    expect(service.isAuthenticated()).toBeFalse();
    expect(sessionStorage.getItem('sigma.session')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
