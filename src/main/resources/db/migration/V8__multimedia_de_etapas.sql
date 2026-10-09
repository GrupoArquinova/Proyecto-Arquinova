-- Imágenes de cada etapa: se muestran en la tarjeta que se abre al pulsar el botón de la etapa sobre el plano.
-- Al borrar la etapa se borran sus imágenes (igual que con lotes, zonas comunes y tipologías).
--
-- MySQL exige que la columna de la clave foránea tenga EXACTAMENTE el mismo tipo que la columna a la que apunta
-- (por ejemplo, "bigint" con signo no sirve contra "bigint unsigned"). Como ese tipo cambia según cómo se creó
-- cada base de datos, se copia el de etapas.id en lugar de escribirlo a mano.
SET @tipo_id = (SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'etapas' AND COLUMN_NAME = 'id');

SET @alterar = CONCAT(
    'ALTER TABLE multimedia ',
    'ADD COLUMN etapa_id ', @tipo_id, ' NULL, ',
    'ADD INDEX idx_multimedia_etapa (etapa_id, orden), ',
    'ADD CONSTRAINT fk_multimedia_etapa FOREIGN KEY (etapa_id) REFERENCES etapas(id) ON DELETE CASCADE');

PREPARE instruccion FROM @alterar;
EXECUTE instruccion;
DEALLOCATE PREPARE instruccion;
