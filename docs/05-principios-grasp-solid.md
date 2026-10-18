
# Aplicación de Patrones GRASP y Principios SOLID

## 1. Patrones GRASP
| Patrón | Clase / Método | Justificación de diseño |
| :--- | :--- | :--- |
| **Information Expert** | `ReglasBola8.evaluar()` | Se asigna la responsabilidad de evaluar un tiro a la clase que tiene toda la información sobre las reglas de Bola 8. |
| **Creator** | `ReglasFactory.crear()` | La fábrica es la encargada de crear las instancias de `ReglasBillar` según la modalidad, centralizando la lógica de instanciación. |
| **Controller** | `MatchController` | Actúa como el primer punto de contacto para los eventos del sistema (peticiones HTTP), delegando la lógica al servicio. |
| **Low Coupling** (Bajo Acoplamiento) | Módulo `billiard-api` | El backend depende de las reglas, pero las reglas **no** dependen del backend. Esto permite cambiar de REST a WebSockets sin tocar las reglas. |
| **High Cohesion** (Alta Cohesión) | `MatchService` | Esta clase se encarga *únicamente* de orquestar la creación de partidas y el registro de tiros. No sabe de SQL ni de HTTP. |

## 2. Principios SOLID
| Principio | Clase / Relación | Justificación de diseño |
| :--- | :--- | :--- |
| **SRP** (Single Responsibility) | `MatchState` | Tiene una única responsabilidad: mantener el estado actual de la partida (puntajes, turnos, tiros). No evalúa reglas, solo almacena datos. |
| **OCP** (Open/Closed) | `AbstractReglasBillar` | El sistema está abierto a extensión (si mañana se agrega la modalidad Snooker, se crea `ReglasSnooker`) pero cerrado a modificación (no hay que tocar `ReglasBola8` ni `ReglasPorPuntos`). |
| **LSP** (Liskov Substitution) | `ReglasBola8`, `ReglasPorPuntos` | Cualquier instancia de estas clases puede ser usada en lugar de la interfaz `ReglasBillar` sin alterar el correcto funcionamiento del sistema. |
| **ISP** (Interface Segregation) | Interfaz `ReglasBillar` | La interfaz tiene solo los métodos necesarios (`evaluar`, `partidaTerminada`). No obliga a las clases a implementar métodos que no usan. |
| **DIP** (Dependency Inversion) | `MatchService` | El servicio depende de la abstracción (`ReglasBillar`), no de las implementaciones concretas (`ReglasBola8` o `ReglasPorPuntos`). |
