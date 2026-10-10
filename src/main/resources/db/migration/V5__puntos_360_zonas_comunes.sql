-- Los botones sobre la imagen de "Zonas destacadas" apuntan a una zona común.
-- Sin clave foránea, igual que lote_id y etapa_id (ver V4): Punto360Service omite los puntos huérfanos.
ALTER TABLE puntos_360
    ADD COLUMN zona_comun_id BIGINT NULL,
    ADD INDEX idx_puntos_360_zona (zona_comun_id);
