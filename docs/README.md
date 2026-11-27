# Documentación de PoolHawk

Documentación técnica del proyecto. Los documentos describen el corte de referencia `43ae3cba25f5dcdead273982cb1f810df0222ba8`, salvo que cada archivo indique otra cosa.

## Documentos principales

1. [Contexto del proyecto](01-contexto-del-proyecto.md) — propósito, stack, módulos y estado conocido.
2. [Reglas y decisiones](02-reglas-del-proyecto.md) — reglas de negocio, arquitectura y contratos que deben preservarse.
3. [Design system](03-design-system.md) — identidad visual y criterios de interfaz.
4. [Arquitectura y paquetes](04-arquitectura-paquetes.md) — módulos backend, frontend, agente e infraestructura.
5. [GRASP y SOLID](05-principios-grasp-solid.md) — aplicación y trazabilidad de principios.
6. [Casos de uso](06-casos-de-uso.md) — actores y flujos funcionales.
7. [Estado de auditoría técnica](07-estado-auditoria.md) — hallazgos, mejoras, prioridades y pruebas pendientes.

## Diagramas

- [Diagrama de clases](diagramas/01-clases.md)
- [Secuencia de registro de tiro](diagramas/02-secuencia-tiro.md)
- [Actividades de procesamiento de tiro](diagramas/03-actividades-tiro.md)
- [Componentes del sistema](diagramas/04-componentes.md)
- [Modelo de datos / MER conceptual](diagramas/05-entidad-relacion.md)

## Convenciones de mantenimiento

- Actualizar estos documentos en el mismo Pull Request que cambie arquitectura, contratos o flujos relevantes.
- No documentar una simulación de interfaz como integración real.
- Distinguir implementación confirmada, comportamiento esperado y trabajo pendiente.
- No inventar tablas, clases, relaciones, resultados de pruebas ni métricas.
- Registrar el commit de referencia cuando se realice una auditoría.
