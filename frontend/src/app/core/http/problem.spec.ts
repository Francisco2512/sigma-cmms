import { HttpErrorResponse } from '@angular/common/http';
import { problemMessage } from './problem';

describe('problemMessage', () => {
  it('should_useDetail_when_backendReturnsProblemDetail', () => {
    // Arrange
    const error = new HttpErrorResponse({ status: 422, error: { title: 'Regla de negocio', detail: 'Existencia insuficiente' } });

    // Act + Assert
    expect(problemMessage(error)).toBe('Existencia insuficiente');
  });

  it('should_joinFieldErrors_when_validationFails', () => {
    // Arrange
    const error = new HttpErrorResponse({ status: 400, error: { errors: { title: 'es obligatorio', assetId: 'es obligatorio' } } });

    // Act + Assert
    expect(problemMessage(error)).toBe('es obligatorio. es obligatorio');
  });

  it('should_reportConnectionProblem_when_statusIsZero', () => {
    // Act + Assert
    expect(problemMessage(new HttpErrorResponse({ status: 0 }))).toBe('No hay conexión con el servidor');
  });

  it('should_returnGenericMessage_when_errorIsNotHttp', () => {
    // Act + Assert
    expect(problemMessage(new Error('x'))).toBe('Ocurrió un error inesperado');
  });
});
