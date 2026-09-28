# 004. Jasmine y Karma para las pruebas del frontend

- Estado: aceptada (sustituye lo planteado en la entrega parcial)
- Fecha: 2026-09-21

## Contexto
La entrega parcial proponía Jest para las pruebas del cliente. Angular 20 integra
Jasmine y Karma en su CLI, y el estándar del equipo para Angular es Jasmine con
`ComponentFixture` y `jasmine.createSpyObj`.

## Decisión
Se usa Jasmine + Karma (Chrome sin interfaz) con reporte de cobertura de Istanbul.

## Consecuencias
- Cero configuración adicional sobre el CLI de Angular.
- Cambio registrado conforme al procedimiento de gestión de cambios del proyecto.
