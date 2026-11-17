-- V2: endurecer restricciones NOT NULL sobre la tabla `tiros` (H-04).
--
-- Estrategia segura:
-- Si hay datos antiguos con campos nulos, esta migración fallará.
-- El operador debe auditar manualmente los registros incompatibles antes de forzar
-- la restricción. No se deben borrar datos históricos automáticamente.

ALTER TABLE tiros ALTER COLUMN bolas_embolsadas_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN troneras_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN resultado_json SET NOT NULL;