-- V2: endurecer restricciones NOT NULL sobre la tabla `tiros`.
--
-- Estrategia de migración segura:
-- Si existen registros históricos con campos nulos, este ALTER fallará de forma
-- controlada. Ese fallo es intencional: evita dejar el historial en un estado
-- inconsistente de forma silenciosa.
--
-- Procedimiento ante un fallo:
--   1. Diagnosticar los registros afectados (SELECT id, event_id FROM tiros
--      WHERE resultado_json IS NULL OR bolas_embolsadas_json IS NULL
--      OR troneras_json IS NULL).
--   2. Respaldar la tabla completa antes de cualquier intervención.
--   3. Reparar explícitamente los registros incompletos y verificar el resultado.
--   4. Reejecutar la migración.
--
-- NO se recomienda el borrado masivo de tiros históricos: el historial de la
-- partida es un registro auditable y forma parte del dominio.

ALTER TABLE tiros ALTER COLUMN bolas_embolsadas_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN troneras_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN resultado_json SET NOT NULL;