-- V2: endurecer restricciones NOT NULL sobre la tabla `tiros` (H-04).
-- Estrategia segura de migración:
-- Si existen registros históricos con campos nulos, este ALTER fallará intencionalmente 
-- para evitar corrupción silenciosa de datos. 
-- En tal caso, se requiere un diagnóstico manual, respaldo de los registros afectados 
-- y una reparación explícita de los datos antes de volver a ejecutar la migración.
-- NO se recomienda el borrado masivo (DELETE) en entornos de producción sin auditoría previa.

ALTER TABLE tiros ALTER COLUMN bolas_embolsadas_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN troneras_json SET NOT NULL;
ALTER TABLE tiros ALTER COLUMN resultado_json SET NOT NULL;