-- La solicitud de contacto puede indicar la tipología (villa) por la que se pregunta, además del proyecto y el lote.
-- Sin llave foránea: al eliminar una tipología, CasaModeloService deja en blanco esa referencia en las solicitudes.
ALTER TABLE solicitudes_contacto ADD COLUMN casa_modelo_id BIGINT NULL;
