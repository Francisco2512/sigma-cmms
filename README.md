# SIGMA · Sistema Integral de Gestión de Mantenimiento de Activos

Prototipo funcional de un CMMS (*Computerized Maintenance Management System*) desarrollado
como proyecto de la materia **INGE-00031-1207 Ingeniería de Software**. Centraliza el ciclo
del mantenimiento industrial: activos, órdenes de trabajo, planes preventivos con generación
automática, inventario de refacciones y un tablero con MTBF, MTTR y disponibilidad.

## Arquitectura

```
Angular 20 (SPA)  ──HTTP/JSON + JWT──▶  Spring Boot 4.1 (API REST)  ──JPA/Flyway──▶  MySQL 9
 frontend/                               backend/
```

- **Backend** en capas: `controller` → `services` → `repositories` → `model`. Las transiciones
  de estado de una orden viven en la entidad `WorkOrder`; el consumo de refacciones usa
  bloqueo pesimista (`SELECT ... FOR UPDATE`) para evitar sobreconsumo concurrente.
- **Frontend** con componentes standalone, signals, `OnPush` y carga diferida por ruta.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 20.19+ o 22.12+ |
| MySQL | 8.0+ |

## Puesta en marcha

1. **Base de datos.** Con un usuario administrador de MySQL:

   ```sql
   CREATE DATABASE sigma_cmms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE DATABASE sigma_cmms_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'sigma'@'localhost' IDENTIFIED BY 'la-contrasena-que-elija';
   GRANT ALL PRIVILEGES ON sigma_cmms.* TO 'sigma'@'localhost';
   GRANT ALL PRIVILEGES ON sigma_cmms_test.* TO 'sigma'@'localhost';
   ```

   El esquema (tablas, índices y restricciones) lo crea Flyway al arrancar el backend, a partir de
   `backend/src/main/resources/db/migration/V1__esquema_inicial.sql`.

2. **Secretos locales.** Copie `backend/.env.example` como `backend/.env` y complete los valores:
   la contraseña de la base, una clave JWT de al menos 32 caracteres y la contraseña de los
   usuarios de demostración. El archivo `.env` está ignorado por git.

3. **Backend** (Flyway crea el esquema y se cargan datos de demostración):

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   API en `http://localhost:8080` · documentación en `http://localhost:8080/swagger-ui.html`

4. **Frontend**:

   ```bash
   cd frontend
   npm install
   npm start
   ```

   Aplicación en `http://localhost:4200`

### Usuarios de demostración

La contraseña de todos está en `backend/.env` (`SIGMA_DEMO_PASSWORD`).

| Usuario | Rol | Qué puede hacer |
|---|---|---|
| `admin` | Administrador | Todo |
| `mruiz` | Jefa de mantenimiento | Tablero, asignar, cancelar, dar de baja |
| `jcastillo` | Planificador | Tablero, activos, planes preventivos, asignar |
| `lhernandez` | Técnico | Reportar fallas, ejecutar y cerrar sus órdenes |
| `atorres` | Técnica | Igual que el anterior |
| `pgomez` | Almacenista | Refacciones y entradas de almacén |

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `SIGMA_DB_URL` | URL JDBC | `jdbc:mysql://localhost:3306/sigma_cmms` |
| `SIGMA_DB_USER` | Usuario de la base | `sigma` |
| `SIGMA_DB_PASSWORD` | Contraseña de la base | — |
| `SIGMA_JWT_SECRET` | Clave HMAC de los tokens (≥ 32 bytes) | aleatoria por arranque |
| `SIGMA_DEMO_PASSWORD` | Contraseña de los usuarios de demostración | — |
| `SIGMA_CORS_ORIGINS` | Orígenes permitidos | `http://localhost:4200` |

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/auth/login` | Inicio de sesión (JWT) |
| GET | `/api/v1/dashboard/kpis?days=90` | MTBF, MTTR, disponibilidad, cumplimiento |
| GET/POST | `/api/v1/work-orders` | Listado filtrable y alta de órdenes |
| PATCH | `/api/v1/work-orders/{id}/assign \| start \| close \| cancel` | Transiciones de estado |
| GET/POST/PUT/DELETE | `/api/v1/assets` | Catálogo de activos e historial |
| GET/POST | `/api/v1/spare-parts` · PATCH `/{id}/stock` | Inventario y entradas |
| GET/POST | `/api/v1/preventive-plans` · POST `/generate` | Planes y generación de órdenes |

Contrato completo en Swagger UI.

## Pruebas

```bash
# Backend: unitarias + capa web (JUnit 5, Mockito, AssertJ, MockMvc) y cobertura JaCoCo
cd backend && ./mvnw test

# Backend: integración contra MySQL real (base sigma_cmms_test)
SIGMA_IT=true ./mvnw test

# Backend: análisis estático (reporte en target/pmd.xml)
./mvnw pmd:pmd

# Frontend: Jasmine + Karma con cobertura
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless --code-coverage
```
