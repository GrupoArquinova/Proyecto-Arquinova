-- =============================================================================
-- V1__initial_schema.sql
-- Migración Inicial de Base de Datos - Grupo Arquinova
-- Incluye DDL completo de entidades e índices de alto rendimiento para producción
-- =============================================================================

-- 1. Roles del Sistema
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Usuarios y Autenticación
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rol_id BIGINT NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    correo VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso_en DATETIME NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_usuarios_correo ON usuarios(correo);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);


-- 3. Tokens de Recuperación de Contraseña
CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expira_en DATETIME NOT NULL,
    usado_en DATETIME NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tokens_expira ON tokens_recuperacion(expira_en);

-- 4. Empresas Constructoras
CREATE TABLE IF NOT EXISTS empresas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    nit VARCHAR(30) UNIQUE,
    descripcion TEXT,
    trayectoria TEXT,
    servicios TEXT,
    correo_comercial VARCHAR(254),
    telefono VARCHAR(30),
    whatsapp VARCHAR(30),
    sitio_web VARCHAR(255),
    direccion VARCHAR(255),
    logo_url VARCHAR(1000),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_empresas_activo ON empresas(activo);

-- 5. Proyectos
CREATE TABLE IF NOT EXISTS proyectos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    descripcion TEXT,
    estado_proyecto VARCHAR(30) NOT NULL DEFAULT 'PLANIFICACION',
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_lanzamiento DATE,
    creado_por BIGINT,
    actualizado_por BIGINT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_proyectos_empresa_slug UNIQUE (empresa_id, slug),
    CONSTRAINT fk_proyectos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_proyectos_creador FOREIGN KEY (creado_por) REFERENCES usuarios(id),
    CONSTRAINT fk_proyectos_actualizador FOREIGN KEY (actualizado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_proyectos_slug ON proyectos(slug);
CREATE INDEX idx_proyectos_publicado_activo ON proyectos(publicado, activo);
CREATE INDEX idx_proyectos_estado ON proyectos(estado_proyecto);


-- 6. Ubicación de Proyectos
CREATE TABLE IF NOT EXISTS ubicaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL UNIQUE,
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    departamento VARCHAR(100),
    referencias TEXT,
    latitud DECIMAL(10, 7),
    longitud DECIMAL(10, 7),
    google_maps_url VARCHAR(1000),
    urbanismo_url VARCHAR(1000),
    vista_aerea_url VARCHAR(1000),
    recorrido_360_url VARCHAR(1000),
    video_como_llegar_url VARCHAR(1000),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ubicaciones_proyecto UNIQUE (proyecto_id),
    CONSTRAINT fk_ubicaciones_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ubicaciones_ciudad_depto ON ubicaciones(ciudad, departamento);

-- 7. Etapas de Proyectos
CREATE TABLE IF NOT EXISTS etapas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    orden SMALLINT NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_etapas_proyecto_nombre UNIQUE (proyecto_id, nombre),
    CONSTRAINT fk_etapas_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_etapas_proyecto ON etapas(proyecto_id, orden);

-- 8. Catálogo Estados de Lote
CREATE TABLE IF NOT EXISTS estados_lote (
    id TINYINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    orden TINYINT NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_estados_lote_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Lotes
CREATE TABLE IF NOT EXISTS lotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    etapa_id BIGINT NOT NULL,
    estado_id TINYINT NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(120),
    area_m2 DECIMAL(10, 2) NOT NULL,
    descripcion TEXT,
    caracteristicas TEXT,
    posicion_x DECIMAL(12, 6),
    posicion_y DECIMAL(12, 6),
    publicado BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por BIGINT,
    actualizado_por BIGINT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_lotes_etapa_codigo UNIQUE (etapa_id, codigo),
    CONSTRAINT fk_lotes_etapa FOREIGN KEY (etapa_id) REFERENCES etapas(id) ON DELETE CASCADE,
    CONSTRAINT fk_lotes_estado FOREIGN KEY (estado_id) REFERENCES estados_lote(id),
    CONSTRAINT fk_lotes_creador FOREIGN KEY (creado_por) REFERENCES usuarios(id),
    CONSTRAINT fk_lotes_actualizador FOREIGN KEY (actualizado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lotes_etapa_estado ON lotes(etapa_id, estado_id);
CREATE INDEX idx_lotes_publicado_activo ON lotes(publicado, activo);

-- 10. Historial de Estados de Lote
CREATE TABLE IF NOT EXISTS historial_estado_lote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lote_id BIGINT NOT NULL,
    estado_anterior_id TINYINT NULL,
    estado_nuevo_id TINYINT NOT NULL,
    cambiado_por BIGINT NOT NULL,
    observacion VARCHAR(500),
    cambiado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_lote FOREIGN KEY (lote_id) REFERENCES lotes(id) ON DELETE CASCADE,
    CONSTRAINT fk_historial_estado_anterior FOREIGN KEY (estado_anterior_id) REFERENCES estados_lote(id),
    CONSTRAINT fk_historial_estado_nuevo FOREIGN KEY (estado_nuevo_id) REFERENCES estados_lote(id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (cambiado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_historial_lote_fecha ON historial_estado_lote(lote_id, cambiado_en);


-- 11. Casas Modelo
CREATE TABLE IF NOT EXISTS casas_modelo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    area_construida_m2 DECIMAL(10, 2),
    numero_habitaciones TINYINT,
    numero_banos TINYINT,
    tour_virtual_url VARCHAR(1000),
    plano_url VARCHAR(1000),
    publicado BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_casas_proyecto_nombre UNIQUE (proyecto_id, nombre),
    CONSTRAINT fk_casas_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_casas_proyecto ON casas_modelo(proyecto_id, publicado, activo);

-- 12. Zonas Comunes
CREATE TABLE IF NOT EXISTS zonas_comunes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    publicado BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_zonas_proyecto_nombre UNIQUE (proyecto_id, nombre),
    CONSTRAINT fk_zonas_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_zonas_proyecto ON zonas_comunes(proyecto_id, activo);

-- 13. Imágenes de Zonas Comunes
CREATE TABLE IF NOT EXISTS zona_comun_imagenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zona_comun_id BIGINT NOT NULL,
    imagen_url VARCHAR(1000) NOT NULL,
    titulo VARCHAR(150),
    orden SMALLINT NOT NULL DEFAULT 1,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_imagenes_zona FOREIGN KEY (zona_comun_id) REFERENCES zonas_comunes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_zona_imagenes_orden ON zona_comun_imagenes(zona_comun_id, orden);

-- 14. Multimedia Centralizada
CREATE TABLE IF NOT EXISTS multimedia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NULL,
    lote_id BIGINT NULL,
    zona_comun_id BIGINT NULL,
    casa_modelo_id BIGINT NULL,
    tipo VARCHAR(20) NOT NULL,
    titulo VARCHAR(200),
    descripcion VARCHAR(500),
    url VARCHAR(1500) NOT NULL,
    nombre_archivo VARCHAR(255),
    mime_type VARCHAR(100),
    tamano_bytes BIGINT,
    orden INT NOT NULL DEFAULT 1,
    portada BOOLEAN NOT NULL DEFAULT FALSE,
    publicado BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por BIGINT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_multimedia_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE,
    CONSTRAINT fk_multimedia_lote FOREIGN KEY (lote_id) REFERENCES lotes(id) ON DELETE CASCADE,
    CONSTRAINT fk_multimedia_zona FOREIGN KEY (zona_comun_id) REFERENCES zonas_comunes(id) ON DELETE CASCADE,
    CONSTRAINT fk_multimedia_casa FOREIGN KEY (casa_modelo_id) REFERENCES casas_modelo(id) ON DELETE CASCADE,
    CONSTRAINT fk_multimedia_usuario FOREIGN KEY (creado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_multimedia_proyecto ON multimedia(proyecto_id, orden);
CREATE INDEX idx_multimedia_lote ON multimedia(lote_id, orden);
CREATE INDEX idx_multimedia_zona ON multimedia(zona_comun_id, orden);
CREATE INDEX idx_multimedia_casa ON multimedia(casa_modelo_id, orden);
CREATE INDEX idx_multimedia_publicado ON multimedia(publicado, activo);

-- 15. Estados de Solicitud de Contacto
CREATE TABLE IF NOT EXISTS estados_solicitud (
    id TINYINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    orden TINYINT NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_estados_solicitud_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_estados_solicitud_activo ON estados_solicitud(activo);

-- 16. Solicitudes de Contacto / Leads
CREATE TABLE IF NOT EXISTS solicitudes_contacto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    estado_id TINYINT NOT NULL,
    proyecto_id BIGINT NULL,
    lote_id BIGINT NULL,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(254) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    mensaje TEXT,
    consentimiento_datos BOOLEAN NOT NULL DEFAULT FALSE,
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    atendida_por BIGINT NULL,
    atendida_en DATETIME NULL,
    observaciones_internas TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_solicitudes_estado FOREIGN KEY (estado_id) REFERENCES estados_solicitud(id),
    CONSTRAINT fk_solicitudes_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE SET NULL,
    CONSTRAINT fk_solicitudes_lote FOREIGN KEY (lote_id) REFERENCES lotes(id) ON DELETE SET NULL,
    CONSTRAINT fk_solicitudes_atendida_por FOREIGN KEY (atendida_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_solicitudes_estado_fecha ON solicitudes_contacto(estado_id, creado_en);
CREATE INDEX idx_solicitudes_fecha ON solicitudes_contacto(creado_en);

-- 17. Contenidos Institucionales
CREATE TABLE IF NOT EXISTS contenidos_institucionales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    seccion VARCHAR(80) NOT NULL,
    titulo VARCHAR(200),
    contenido TEXT,
    publicado BOOLEAN NOT NULL DEFAULT TRUE,
    actualizado_por BIGINT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_contenidos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_contenidos_usuario FOREIGN KEY (actualizado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_contenidos_empresa_seccion ON contenidos_institucionales(empresa_id, seccion);

-- 18. Registro de Auditoría
CREATE TABLE IF NOT EXISTS auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL,
    accion VARCHAR(50) NOT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_id BIGINT NULL,
    descripcion VARCHAR(500),
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auditoria_fecha ON auditoria(creado_en);
CREATE INDEX idx_auditoria_entidad ON auditoria(entidad, entidad_id);

