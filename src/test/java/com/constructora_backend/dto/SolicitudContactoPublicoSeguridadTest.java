package com.constructora_backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Formulario público: tamaños, caracteres de control, campos que el servidor fija y campo trampa. */
class SolicitudContactoPublicoSeguridadTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private SolicitudContactoPublicDTO valida() {
        SolicitudContactoPublicDTO dto = new SolicitudContactoPublicDTO();
        dto.setNombre("Ana Gómez");
        dto.setCorreo("ana@example.com");
        dto.setTelefono("+57 300 123 4567");
        dto.setServicioInteres("Licencias y permisos");
        dto.setMensaje("Quiero información");
        dto.setConsentimientoDatos(true);
        return dto;
    }

    private boolean fallaEn(SolicitudContactoPublicDTO dto, String campo) {
        Set<ConstraintViolation<SolicitudContactoPublicDTO>> errores = validator.validate(dto);
        return errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals(campo));
    }

    @Test
    @DisplayName("Una solicitud normal es válida")
    void laNormalEsValida() {
        assertTrue(validator.validate(valida()).isEmpty());
    }

    @Test
    @DisplayName("El mensaje admite hasta 2000 caracteres (con saltos de línea) y rechaza más")
    void tamanoDelMensaje() {
        SolicitudContactoPublicDTO dto = valida();
        dto.setMensaje("línea uno\nlínea dos\r\ntercera");
        assertFalse(fallaEn(dto, "mensaje"), "un mensaje de varias líneas es normal");
        dto.setMensaje("a".repeat(2000));
        assertFalse(fallaEn(dto, "mensaje"));

        dto.setMensaje("a".repeat(2001));
        assertTrue(fallaEn(dto, "mensaje"));
    }

    @Test
    @DisplayName("Nombre, teléfono y servicio no admiten saltos de línea ni caracteres de control (inyección de cabeceras de correo)")
    void sinCaracteresDeControl() {
        for (String malo : new String[]{"Ana\r\nBcc: victima@x.com", "Ana\nSubject: x", "Ana\tTab", "Ana\u0000Nulo", "Ana\u007f"}) {
            SolicitudContactoPublicDTO dto = valida();
            dto.setNombre(malo);
            assertTrue(fallaEn(dto, "nombre"), "nombre: " + malo.replaceAll("\\p{Cntrl}", "?"));

            dto = valida();
            dto.setTelefono(malo);
            assertTrue(fallaEn(dto, "telefono"), "teléfono");

            dto = valida();
            dto.setServicioInteres(malo);
            assertTrue(fallaEn(dto, "servicioInteres"), "servicio");
        }
    }

    @Test
    @DisplayName("Los límites de largo de nombre (150), teléfono (30) y servicio (120) se respetan")
    void limitesDeLargo() {
        SolicitudContactoPublicDTO dto = valida();
        dto.setNombre("a".repeat(151));
        assertTrue(fallaEn(dto, "nombre"));

        dto = valida();
        dto.setTelefono("1".repeat(31));
        assertTrue(fallaEn(dto, "telefono"));

        dto = valida();
        dto.setServicioInteres("a".repeat(121));
        assertTrue(fallaEn(dto, "servicioInteres"));
    }

    @Test
    @DisplayName("Si el cliente manda ip o userAgent en el JSON se ignoran (los fija el servidor); el DTO ya no los tiene")
    void ipYUserAgentDelClienteSeIgnoran() throws Exception {
        String json = "{\"nombre\":\"Ana\",\"correo\":\"ana@example.com\",\"consentimientoDatos\":true,"
                + "\"ip\":\"6.6.6.6\",\"userAgent\":\"falso\",\"campoDesconocido\":1}";

        SolicitudContactoPublicDTO dto = new ObjectMapper().readValue(json, SolicitudContactoPublicDTO.class);

        assertEquals("Ana", dto.getNombre());
        assertTrue(java.util.Arrays.stream(SolicitudContactoPublicDTO.class.getDeclaredFields())
                .noneMatch(f -> f.getName().equals("ip") || f.getName().equals("userAgent")));
    }

    @Test
    @DisplayName("Campo trampa sitioWeb: vacío o ausente es una persona; con texto es un robot")
    void campoTrampa() {
        SolicitudContactoPublicDTO dto = valida();
        assertFalse(dto.esRobot());

        dto.setSitioWeb("");
        assertFalse(dto.esRobot());
        dto.setSitioWeb("   ");
        assertFalse(dto.esRobot());

        dto.setSitioWeb("http://spam.example");
        assertTrue(dto.esRobot());
        assertTrue(validator.validate(dto).isEmpty(), "el robot no recibe un 400 que lo avise: pasa la validación");
    }

    @Test
    @DisplayName("El campo trampa llega desde el JSON del formulario")
    void campoTrampaDesdeJson() throws Exception {
        SolicitudContactoPublicDTO dto = new ObjectMapper()
                .readValue("{\"nombre\":\"Bot\",\"telefono\":\"3001234567\",\"consentimientoDatos\":true,\"sitioWeb\":\"spam\"}",
                        SolicitudContactoPublicDTO.class);

        assertTrue(dto.esRobot());
    }
}
