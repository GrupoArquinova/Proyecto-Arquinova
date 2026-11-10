-- =============================================================================
-- DATOS INICIALES PARA UNA BASE LOCAL NUEVA (constructora_web)
--
-- Ejecutar UNA VEZ, despues de que el backend arranco con FLYWAY_ENABLED=true y creo las tablas.
-- Se puede repetir sin duplicar datos. No borra nada.
--
-- Crea: el rol ADMINISTRADOR, los estados de lote, la empresa (id 1) y un administrador para entrar al panel:
--     correo: admin@arquinova.com        clave: Arquinova#Local1
--
-- ATENCION: esa clave es SOLO PARA TU COMPUTADOR. Cambiala en el panel apenas entres, y NUNCA uses este archivo
-- en una base publicada en internet (Aiven, un servidor, etc.).
-- Los estados de las solicitudes y los roles COMERCIAL y EDITOR ya los crean las migraciones del backend.
-- =============================================================================
USE constructora_web;

INSERT INTO roles (id, nombre, descripcion, activo) VALUES
 (1, 'ADMINISTRADOR', 'Administrador principal del sistema', TRUE)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

INSERT INTO estados_lote (id, nombre, descripcion, orden, activo) VALUES
 (1, 'DISPONIBLE', 'Lote disponible para comercialización', 1, TRUE),
 (2, 'RESERVADO',  'Lote reservado por un cliente', 2, TRUE),
 (3, 'VENDIDO',    'Lote vendido', 3, TRUE)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion), orden = VALUES(orden), activo = TRUE;

INSERT INTO empresas (id, nombre, nit, descripcion, correo_comercial, telefono, whatsapp, direccion, activo) VALUES
 (1, 'Grupo Arquinova S.A.S.', '901.397.504-2',
  'Empresa con sede en Armenia que coordina servicios de arquitectura, ingeniería y gestión de proyectos.',
  'grupoarquinova1@gmail.com', '+57 316 865 3715', '+57 316 865 3715', 'Carrera 14 #48N-58, Armenia, Quindío', TRUE)
ON DUPLICATE KEY UPDATE activo = TRUE;

INSERT INTO usuarios (rol_id, nombre_completo, correo, password_hash, activo)
SELECT 1, 'Administrador Arquinova', 'admin@arquinova.com',
       '$2a$10$.ZaBB8nqonUfh9sl0g8dC.9cVenOGi5yTvGZnJ0ur8HZidqYafy1i', TRUE
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE correo = 'admin@arquinova.com');

SELECT id, correo, activo FROM usuarios;
