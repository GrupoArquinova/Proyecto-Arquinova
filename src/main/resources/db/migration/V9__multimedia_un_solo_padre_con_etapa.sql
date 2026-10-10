-- Un recurso multimedia pertenece a EXACTAMENTE una cosa: proyecto, lote, zona común, tipología o (desde V8) etapa.
-- Algunas bases (por ejemplo la local, restaurada desde una copia) ya tienen esta regla sin la etapa y rechazan los
-- recursos de etapa; otras no la tienen. Por eso primero se quita solo si existe y después se crea completa.
SET @existe = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'chk_multimedia_un_solo_padre');

SET @quitar = IF(@existe > 0,
                 'ALTER TABLE multimedia DROP CONSTRAINT chk_multimedia_un_solo_padre',
                 'SELECT 1');
PREPARE instruccion FROM @quitar;
EXECUTE instruccion;
DEALLOCATE PREPARE instruccion;

ALTER TABLE multimedia
    ADD CONSTRAINT chk_multimedia_un_solo_padre CHECK (
        (proyecto_id IS NOT NULL) + (lote_id IS NOT NULL) + (zona_comun_id IS NOT NULL)
        + (casa_modelo_id IS NOT NULL) + (etapa_id IS NOT NULL) = 1);
