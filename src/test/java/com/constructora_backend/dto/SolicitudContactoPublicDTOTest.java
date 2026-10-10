package com.constructora_backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudContactoPublicDTOTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private SolicitudContactoPublicDTO solicitud(String correo, String telefono) {
        SolicitudContactoPublicDTO dto = new SolicitudContactoPublicDTO();
        dto.setNombre("Ana Gómez");
        dto.setCorreo(correo);
        dto.setTelefono(telefono);
        dto.setMensaje("Quiero información");
        dto.setConsentimientoDatos(true);
        return dto;
    }

    @Test
    void conSoloTelefonoEsValida() {
        assertTrue(validator.validate(solicitud(null, "3001234567")).isEmpty());
    }

    @Test
    void conSoloCorreoEsValida() {
        assertTrue(validator.validate(solicitud("ana@example.com", null)).isEmpty());
    }

    @Test
    void conAmbosMediosEsValida() {
        assertTrue(validator.validate(solicitud("ana@example.com", "3001234567")).isEmpty());
    }

    @Test
    void sinNingunMedioDeContactoNoEsValida() {
        assertFalse(validator.validate(solicitud(null, null)).isEmpty());
        assertFalse(validator.validate(solicitud("", "   ")).isEmpty());
    }

    @Test
    void unCorreoMalEscritoNoEsValido() {
        assertFalse(validator.validate(solicitud("no-es-un-correo", "3001234567")).isEmpty());
    }

    @Test
    void sinConsentimientoNoEsValida() {
        SolicitudContactoPublicDTO dto = solicitud("ana@example.com", null);
        dto.setConsentimientoDatos(false);
        assertFalse(validator.validate(dto).isEmpty());
    }
}
