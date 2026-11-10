-- Botones (puntos) sobre la imagen 360° del entorno, la vista aérea y el plano de urbanismo.
-- ENTORNO / AEREA se ubican por ángulos (yaw, pitch en radianes); URBANISMO, por porcentaje (pos_x, pos_y).
--
-- Sin claves foráneas a propósito: en bases creadas por Hibernate (tablas sin motor o tipos de id distintos
-- a los de V1) MySQL rechaza la clave con "errno: 150" y la migración queda a medias. La integridad se
-- resuelve en la aplicación: Punto360Service omite los puntos cuyo lote o etapa ya no existe.
CREATE TABLE IF NOT EXISTS puntos_360 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL,
    escena VARCHAR(20) NOT NULL,
    lote_id BIGINT NULL,
    etapa_id BIGINT NULL,
    etiqueta VARCHAR(120) NOT NULL,
    yaw DECIMAL(9, 6) NULL,
    pitch DECIMAL(9, 6) NULL,
    pos_x DECIMAL(6, 3) NULL,
    pos_y DECIMAL(6, 3) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_puntos_360_proyecto (proyecto_id, escena),
    INDEX idx_puntos_360_lote (lote_id),
    INDEX idx_puntos_360_etapa (etapa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
