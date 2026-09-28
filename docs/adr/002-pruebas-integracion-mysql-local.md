# 002. Pruebas de integración contra MySQL local

- Estado: aceptada
- Fecha: 2026-09-21

## Contexto
La norma del equipo pide Testcontainers para las pruebas de integración con MySQL.
El equipo de desarrollo no cuenta con Docker, requisito de Testcontainers.

## Decisión
Las pruebas de integración usan una base dedicada `sigma_cmms_test` en el MySQL local,
que se limpia y migra con Flyway antes de cada prueba. Se activan con `SIGMA_IT=true`
para que el build no falle en máquinas sin esa base. No se usa H2: el comportamiento
debe ser el del motor real (bloqueos `SELECT ... FOR UPDATE`, restricciones `CHECK`).

## Consecuencias
- Se prueba contra el motor real, incluida la reversión transaccional.
- Requiere preparar la base una vez (`database/setup-local.sql`).
- Con Docker disponible, sustituir la base local por un contenedor de Testcontainers.
