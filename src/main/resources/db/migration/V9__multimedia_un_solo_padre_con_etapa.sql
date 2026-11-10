-- Un recurso multimedia pertenece a EXACTAMENTE una cosa: proyecto, lote, zona común, tipología o (desde V8) etapa.
--
-- Esta regla se valida SIEMPRE en la aplicación (Multimedia.validarUnSoloPadre). Aquí se agrega además como restricción CHECK
-- de la base de datos, donde se pueda:
--   * MariaDB (XAMPP, bases locales): permite el CHECK aunque las columnas tengan clave foránea con ON DELETE CASCADE.
--   * MySQL 8 (Aiven, Kubernetes): NO lo permite (error 3823: una columna con clave foránea y acción referencial,
--     como CASCADE, no puede usarse en un CHECK). Allí la regla queda solo en la aplicación y la migración no falla.
-- Algunas bases ya tienen la regla sin la etapa y rechazarían los recursos de etapa; por eso primero se quita si existe.

SET @existe = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'chk_multimedia_un_solo_padre');

SET @quitar = IF(@existe > 0,
                 'ALTER TABLE multimedia DROP CONSTRAINT chk_multimedia_un_solo_padre',
                 'SELECT 1');
PREPARE instruccion FROM @quitar;
EXECUTE instruccion;
DEALLOCATE PREPARE instruccion;

SET @es_mariadb = (SELECT VERSION() LIKE '%MariaDB%');
SET @fk_con_accion = (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
                      WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'multimedia'
                        AND (DELETE_RULE NOT IN ('RESTRICT', 'NO ACTION') OR UPDATE_RULE NOT IN ('RESTRICT', 'NO ACTION')));

SET @crear = IF(@es_mariadb = 1 OR @fk_con_accion = 0,
                'ALTER TABLE multimedia ADD CONSTRAINT chk_multimedia_un_solo_padre CHECK ((proyecto_id IS NOT NULL) + (lote_id IS NOT NULL) + (zona_comun_id IS NOT NULL) + (casa_modelo_id IS NOT NULL) + (etapa_id IS NOT NULL) = 1)',
                'SELECT 1');
PREPARE instruccion FROM @crear;
EXECUTE instruccion;
DEALLOCATE PREPARE instruccion;
