import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], { token: 'jwt-123' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('should_addBearerToken_when_callingTheApi', () => {
    // Act
    http.get('/api/v1/assets').subscribe();

    // Assert
    const request = controller.expectOne('/api/v1/assets');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    request.flush({});
  });

  it('should_notSendToken_when_callingExternalUrl', () => {
    // Act
    http.get('https://example.com/data').subscribe();

    // Assert
    const request = controller.expectOne('https://example.com/data');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('should_logout_when_apiRespondsUnauthorized', () => {
    // Act
    http.get('/api/v1/assets').subscribe({ error: () => undefined });
    controller.expectOne('/api/v1/assets').flush({}, { status: 401, statusText: 'Unauthorized' });

    // Assert
    expect(auth.logout).toHaveBeenCalled();
  });
});
