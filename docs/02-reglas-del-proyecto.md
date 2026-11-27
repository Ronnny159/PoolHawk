# Reglas y decisiones del proyecto

## 1. Decisiones de arquitectura que deben preservarse

1. **Separación del dominio:** `billiard-domain` contiene el modelo del negocio y debe mantenerse independiente de Spring, JPA, HTTP y detalles de infraestructura.
2. **Motor de reglas desacoplado:** `ReglasBillar` define el contrato; `ReglasBola8` y `ReglasPorPuntos` implementan comportamientos específicos a través de la estructura base `AbstractReglasBillar`.
3. **Factory de reglas:** `ReglasFactory` selecciona la estrategia correspondiente a la modalidad.
4. **API como adaptador:** los controladores reciben solicitudes, validan el contrato y delegan la operación a servicios. No deben contener reglas de billar.
5. **Persistencia separada:** el dominio se transforma a/desde entidades de persistencia mediante mappers; el acceso se coordina desde almacenamiento/repositorios.
6. **Backend como autoridad:** el agente visual propone eventos; el backend comprueba turno, versión, reglas y consistencia antes de aplicar un tiro.
7. **Eventos después de commit:** las notificaciones de una partida solo deben emitirse después de confirmar la transacción que persiste el nuevo estado.
8. **Configuración por entorno:** secretos, credenciales y URLs no deben quedar codificados en componentes o archivos versionados.

## 2. Reglas de negocio documentadas

### Bola 8
- Participan dos jugadores (1 contra 1).
- Los grupos de bolas son lisas (1–7) y rayadas (9–15).
- La bola 8 se debe embolsar después de completar el grupo asignado.
- Las condiciones de falta y victoria deben mantenerse en `ReglasBola8` y sus pruebas, no duplicarse en el frontend.

### Por Puntos
- El sistema contempla formatos individual y por parejas.
- La configuración de puntuación y condición de victoria debe provenir de la regla/configuración vigente en el código. No asumir un puntaje fijo distinto al definido por `ReglasPorPuntos` y `ReglasConfig`.

### Validación de tiros
- Se valida que exista la partida.
- Se valida que el evento incluya un `eventId`.
- Se valida que el jugador que envía el tiro sea el jugador actual.
- Se validan bolas existentes, no repetidas y todavía en mesa.
- La cantidad de troneras debe corresponder a la cantidad de bolas embolsadas.
- Se valida la versión esperada para detectar estado desactualizado.
- Un mismo `eventId` con el mismo payload puede resolverse de forma idempotente; reutilizarlo con un payload diferente debe rechazarse.
- El backend evalúa el evento con la estrategia de reglas de la partida.

## 3. Persistencia y concurrencia

- PostgreSQL es el motor relacional del entorno.
- Las partidas se almacenan mediante entidades JPA y componentes de almacenamiento.
- `PartidaEntity` utiliza `@Version` para control optimista de concurrencia.
- El servicio carga un estado fresco desde la base de datos antes de registrar un tiro.
- Los conflictos de versión se deben comunicar como conflicto y no sobrescribir silenciosamente el estado.
- La caché es auxiliar; la base de datos es la fuente de verdad.

## 4. Integración y comunicación

- Frontend ↔ backend: REST/JSON.
- Agente ↔ backend: REST/JSON con token cuando el endpoint lo exige.
- Backend ↔ clientes conectados: WebSocket.
- Las sesiones WebSocket deben asociarse a partidas mediante una suscripción autorizada; no basta con autenticar el handshake.
- No colocar JWT de larga duración en URLs o comandos compartidos. Preferir credenciales de servicio o enlaces/tickets temporales con alcance limitado.

## 5. Reglas del agente de visión

- El pipeline es: fuente de video → detección → clasificación → seguimiento → análisis de tiro.
- La calibración de mesa, ROI y troneras es necesaria para interpretar correctamente las posiciones.
- Una desaparición o ausencia de detección no prueba por sí sola que una bola haya sido embolsada.
- Los tiros dudosos, sin calibración suficiente o con evidencia ambigua deben requerir revisión manual.
- La cola de eventos debe conservar los envíos pendientes y distinguir errores recuperables, conflictos y rechazos definitivos.
- La trazabilidad debe conservar el identificador del evento y las transiciones de su estado.

## 6. Convenciones de código

- Mantener nombres de clases en PascalCase y métodos/variables en camelCase.
- Mantener responsabilidades acotadas y cohesión alta.
- Validar datos de entrada en los límites del sistema.
- Manejar excepciones con respuestas coherentes y sin ocultar errores.
- Documentar contratos públicos y decisiones no obvias.
- No afirmar que una funcionalidad está implementada si el código solo simula su comportamiento.
