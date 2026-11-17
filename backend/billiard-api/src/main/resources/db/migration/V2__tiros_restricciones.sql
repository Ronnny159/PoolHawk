-- V2: endurecer restricciones NOT NULL sobre la tabla `tiros` (H-04).
--
-- Estrategia segura:
-- Si hay datos antiguos con campos nulos, el ALTER fallará.
-- Si eso ocurre, limpiar la tabla antes de reintentar:
--   DELETE FROM tiros WHERE resultado_json IS NULL;
-- En una BD limpia, los ALTER pasan sin problemas.

ALTER TABLE tiros ALTER COLUMN bolas_embolsadas_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN troneras_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN resultado_json SET NOT NULL;