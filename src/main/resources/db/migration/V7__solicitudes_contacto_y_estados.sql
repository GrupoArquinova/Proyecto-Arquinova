-- Solicitudes de contacto: basta con un medio de contacto (teléfono o correo), servicio de interés
-- y tres estados de atención: NUEVO, EN_GESTION y CERRADO.
ALTER TABLE solicitudes_contacto
    MODIFY COLUMN correo VARCHAR(254) NULL,
    MODIFY COLUMN telefono VARCHAR(30) NULL,
    ADD COLUMN servicio_interes VARCHAR(120) NULL;

-- Estados: la antigua NUEVA pasa a NUEVO; EN_GESTION y CERRADO se crean si no existen.
UPDATE estados_solicitud SET nombre = 'NUEVO', descripcion = 'Solicitud recién recibida', orden = 1, activo = TRUE
 WHERE UPPER(nombre) IN ('NUEVA', 'NUEVO');

INSERT INTO estados_solicitud (nombre, descripcion, orden, activo)
SELECT 'EN_GESTION', 'Solicitud en atención por el equipo', 2, TRUE FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM (SELECT nombre FROM estados_solicitud) e WHERE e.nombre = 'EN_GESTION');

INSERT INTO estados_solicitud (nombre, descripcion, orden, activo)
SELECT 'CERRADO', 'Solicitud finalizada', 3, TRUE FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM (SELECT nombre FROM estados_solicitud) e WHERE e.nombre = 'CERRADO');

-- Las solicitudes que estaban en los estados antiguos se pasan a los nuevos
UPDATE solicitudes_contacto
   SET estado_id = (SELECT id FROM (SELECT id FROM estados_solicitud WHERE nombre = 'EN_GESTION' LIMIT 1) g)
 WHERE estado_id IN (SELECT id FROM (SELECT id FROM estados_solicitud WHERE UPPER(nombre) IN ('CONTACTADA', 'EN_SEGUIMIENTO', 'ATENDIDA')) a);

UPDATE solicitudes_contacto
   SET estado_id = (SELECT id FROM (SELECT id FROM estados_solicitud WHERE nombre = 'CERRADO' LIMIT 1) c)
 WHERE estado_id IN (SELECT id FROM (SELECT id FROM estados_solicitud WHERE UPPER(nombre) IN ('CERRADA')) x);

-- Los estados antiguos quedan inactivos (no se borran, por si algún reporte o historial los menciona)
UPDATE estados_solicitud SET activo = FALSE
 WHERE UPPER(nombre) IN ('CONTACTADA', 'EN_SEGUIMIENTO', 'ATENDIDA', 'CERRADA');
