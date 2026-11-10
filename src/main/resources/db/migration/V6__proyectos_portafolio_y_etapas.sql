-- Proyectos: caso de portafolio u oferta comercial, participación de Arquinova, tipo, destacado y nuevas etapas.
-- La etapa pasa de PLANIFICACION/EN_CONSTRUCCION/ENTREGADO/FINALIZADO a EN_DISENO/EN_TRAMITE/EN_CONSTRUCCION/FINALIZADO.
-- Se guarda como texto (VARCHAR) y la aplicación valida los valores con el enum.
ALTER TABLE proyectos
    MODIFY COLUMN estado_proyecto VARCHAR(30) NOT NULL DEFAULT 'EN_DISENO',
    ADD COLUMN tipo_registro VARCHAR(20) NOT NULL DEFAULT 'OFERTA_COMERCIAL',
    ADD COLUMN tipo_proyecto VARCHAR(20) NULL,
    ADD COLUMN participacion VARCHAR(255) NULL,
    ADD COLUMN destacado BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE proyectos SET estado_proyecto = 'EN_DISENO' WHERE estado_proyecto = 'PLANIFICACION';
UPDATE proyectos SET estado_proyecto = 'FINALIZADO' WHERE estado_proyecto = 'ENTREGADO';
