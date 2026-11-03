-- =============================================================================
-- V14__roles_analitica_chat.sql
-- Roles COMERCIAL y EDITOR, analítica propia sin cookies y preguntas del asistente de IA.
-- Es idempotente: se puede volver a ejecutar sin duplicar nada.
-- =============================================================================

-- 1. Roles nuevos. El nombre se guarda SIN el prefijo ROLE_ (como ADMINISTRADOR); el código lo agrega al autenticar.
--    Llevan los IDs 2 (COMERCIAL) y 3 (EDITOR) cuando están libres (así el ID 1 sigue siendo de ADMINISTRADOR, también en una
--    base nueva donde los catálogos iniciales se cargan después); si algún ID ya está ocupado, el segundo INSERT lo crea con el
--    siguiente ID libre.
INSERT INTO roles (id, nombre, descripcion, activo)
SELECT 2, 'COMERCIAL', 'Atiende las solicitudes de contacto y consulta las métricas (solo lectura)', TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre IN ('COMERCIAL', 'ROLE_COMERCIAL'))
  AND NOT EXISTS (SELECT 1 FROM roles WHERE id = 2);

INSERT INTO roles (nombre, descripcion, activo)
SELECT 'COMERCIAL', 'Atiende las solicitudes de contacto y consulta las métricas (solo lectura)', TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre IN ('COMERCIAL', 'ROLE_COMERCIAL'));

INSERT INTO roles (id, nombre, descripcion, activo)
SELECT 3, 'EDITOR', 'Edita el contenido del sitio: proyectos, lotes, multimedia y contenido institucional', TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre IN ('EDITOR', 'ROLE_EDITOR'))
  AND NOT EXISTS (SELECT 1 FROM roles WHERE id = 3);

INSERT INTO roles (nombre, descripcion, activo)
SELECT 'EDITOR', 'Edita el contenido del sitio: proyectos, lotes, multimedia y contenido institucional', TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre IN ('EDITOR', 'ROLE_EDITOR'));

-- 2. Analítica propia SIN cookies: no guarda IP, user-agent ni identificador de visitante.
--    Sin llave foránea a proyectos (como casa_modelo_id en solicitudes_contacto): los eventos antiguos se depuran por fecha.
CREATE TABLE IF NOT EXISTS eventos_analitica (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    proyecto_id BIGINT NULL,
    ruta VARCHAR(200) NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eventos_analitica_fecha (creado_en),
    INDEX idx_eventos_analitica_tipo_fecha (tipo, creado_en),
    INDEX idx_eventos_analitica_proyecto (proyecto_id, tipo, creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Preguntas del asistente de IA: SOLO la pregunta (sin respuesta, sin IP, sin identificadores).
CREATE TABLE IF NOT EXISTS chat_preguntas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pregunta VARCHAR(500) NOT NULL,
    idioma VARCHAR(5) NULL,
    tema VARCHAR(30) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_preguntas_fecha (creado_en),
    INDEX idx_chat_preguntas_tema_fecha (tema, creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
