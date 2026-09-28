# 003. Análisis estático con PMD y exclusiones

- Estado: aceptada
- Fecha: 2026-09-21

## Contexto
El quality gate de referencia es SonarQube. El entorno local no puede levantar un servidor
SonarQube (requiere Docker o una instalación dedicada).

## Decisión
Se usa PMD (reglas de buenas prácticas, propensión a errores, seguridad y rendimiento)
integrado en Maven, con JaCoCo para la cobertura. Exclusiones:

- `GuardLogStatement`: exige envolver cada log en un `if`. Con SLF4J parametrizado
  (`log.info("... {}", valor)`) el mensaje no se formatea si el nivel está deshabilitado;
  la guarda solo añade ruido.
- Paquete `dto`: registros sin lógica, exclusión ya aceptada en el quality gate.

## Consecuencias
- Primer análisis: 39 hallazgos. Tras correcciones y exclusiones: 2 menores aceptados
  (clase base abstracta intencional y una guarda contra división entre cero).
- En un entorno con SonarQube, el mismo código debe pasar por su quality gate.
