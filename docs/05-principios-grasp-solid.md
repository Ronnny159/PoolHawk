# Aplicación de patrones GRASP y principios SOLID

> Las relaciones descritas corresponden al código inspeccionado en el commit `43ae3cba25f5dcdead273982cb1f810df0222ba8`. La tabla debe actualizarse si cambian las clases en commits posteriores.

## 1. Patrones GRASP

| Patrón | Clase / módulo | Justificación |
|---|---|---|
| Information Expert | `ReglasBola8`, `ReglasPorPuntos` y objetos de dominio | La evaluación de una jugada debe ubicarse en las reglas que conocen las condiciones de la modalidad y el estado del dominio. |
| Creator | `ReglasFactory` | Centraliza la selección de la estrategia de reglas según modalidad. |
| Creator | `MatchFactory` | Construye la partida a partir de la solicitud, evitando concentrar la creación en el servicio de orquestación. |
| Controller | `MatchController`, `EventController` | Reciben solicitudes HTTP y delegan los casos de uso. |
| Low Coupling | Módulos `billiard-domain`, `billiard-rules` y `billiard-api` | El dominio no conoce Spring, JPA ni HTTP; las reglas dependen del dominio. |
| High Cohesion | `MatchPersistenceMapper`, `MatchStorage`, `MatchFactory` | Cada componente concentra una responsabilidad técnica delimitada. |
| Indirection | Mappers y capa de almacenamiento | Separan el modelo del dominio de DTO y entidades de persistencia. |
| Pure Fabrication | `MatchPersistenceMapper`, `MatchStorage` | Servicios técnicos que no representan conceptos del billar, pero mantienen organizado el diseño. |

## 2. Principios SOLID

| Principio | Clase / relación | Evidencia y justificación |
|---|---|---|
| SRP | `MatchFactory`, `MatchPersistenceMapper`, `MatchStorage` | La creación, el mapeo y el almacenamiento están separados del flujo principal de `MatchService`. |
| OCP | `ReglasBillar`, `AbstractReglasBillar`, implementaciones | La estrategia permite incorporar otra modalidad mediante una implementación nueva, siempre que respete el contrato. |
| LSP | `ReglasBola8`, `ReglasPorPuntos` | Las implementaciones deben poder usarse donde se espera `ReglasBillar` sin romper el contrato. |
| ISP | `ReglasBillar` | El contrato se enfoca en las operaciones necesarias para evaluar reglas y determinar el cierre de partida. |
| DIP | `billiard-rules` respecto de `billiard-domain`; abstracción de almacenamiento | Las reglas no dependen de infraestructura web; la persistencia se consume a través de una abstracción de almacenamiento. |

## 3. Trazabilidad adicional

| Decisión / mecanismo | Ubicación | Propósito |
|---|---|---|
| Controlador REST | `MatchController`, `EventController` | Adaptar HTTP a casos de uso. |
| Factory de partidas | `MatchFactory` | Construcción consistente del agregado de partida. |
| Mapper de persistencia | `MatchPersistenceMapper` | Conversión entre dominio y entidad JPA. |
| Almacenamiento | `MatchStorage` | Aislar operaciones de persistencia. |
| Concurrencia optimista | `PartidaEntity` (`@Version`) | Detectar escrituras concurrentes. |
| Idempotencia | `MatchService.registrarTiro` y estado del dominio | Evitar aplicar dos veces el mismo evento. |
| Publicación post-commit | `MatchEventListener` / evento de partida | Evitar notificar un estado que no quedó confirmado. |
| Cola de reintentos | `ColaEventos`, `ReintentadorPendientes` | Conservar eventos no confirmados por problemas de red. |

## 4. Observaciones

La separación de `MatchService` en componentes auxiliares es una mejora de cohesión, pero no significa que el servicio haya dejado de tener responsabilidades de orquestación: continúa coordinando validación, reglas, persistencia, caché y publicación de eventos. La evaluación final de SRP debe considerar el tamaño y las razones de cambio reales del servicio.

Los principios se documentan como evidencia de diseño, no como certificación automática. Para la entrega académica conviene citar métodos concretos y añadir pruebas que demuestren los contratos.
