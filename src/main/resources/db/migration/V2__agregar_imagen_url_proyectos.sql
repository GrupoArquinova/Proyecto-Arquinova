-- ============================================================
-- V2: columna imagen_url en proyectos
-- La entidad Proyecto la usa, pero no estaba en V1, así que una base nueva
-- creada con Flyway fallaba al arrancar (ddl-auto: validate).
-- Es idempotente: si la columna ya existe (agregada a mano), no hace nada.
-- Guarda solo la URL de Cloudinary; el tipo LONGTEXT coincide con la entidad
-- para no romper bases existentes que tengan imágenes antiguas en Base64.
-- ============================================================
SET @existe_columna := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'proyectos'
      AND COLUMN_NAME = 'imagen_url'
);

SET @sentencia := IF(@existe_columna = 0,
    'ALTER TABLE proyectos ADD COLUMN imagen_url LONGTEXT NULL AFTER fecha_lanzamiento',
    'SELECT 1');

PREPARE stmt FROM @sentencia;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
