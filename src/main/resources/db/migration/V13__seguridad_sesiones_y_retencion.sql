-- Seguridad de sesiones: los tokens JWT emitidos antes de este instante dejan de valer.
-- Se actualiza al cambiar o restablecer la contraseña, desactivar la cuenta o cambiar su rol.
ALTER TABLE usuarios ADD COLUMN token_valido_desde DATETIME NULL;

-- Tratamiento de datos (Ley 1581): versión de la política aceptada y momento del consentimiento.
ALTER TABLE solicitudes_contacto
    ADD COLUMN version_politica VARCHAR(20) NULL,
    ADD COLUMN fecha_consentimiento DATETIME NULL;
