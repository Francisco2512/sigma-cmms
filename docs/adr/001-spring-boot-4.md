# 001. Usar Spring Boot 4.1 en el backend

- Estado: aceptada
- Fecha: 2026-09-21

## Contexto
La entrega parcial planteaba un backend Java con Spring Boot. Al iniciar la construcción,
Spring Initializr ya solo ofrece la línea 4.x: la línea 3.x salió del soporte de código abierto.

## Decisión
Se adopta Spring Boot 4.1.1 con Java 21. La autenticación JWT se implementa con el soporte
nativo de Spring Security (OAuth2 Resource Server + Nimbus) en lugar de una librería externa.

## Consecuencias
- Dependencias con soporte vigente y parches de seguridad (OWASP A06).
- Una dependencia menos que mantener para JWT.
- Algunas APIs cambiaron de paquete (por ejemplo `PropertyReferenceException`,
  `@WebMvcTest`, `@MockitoBean`); se ajustaron durante la construcción.
