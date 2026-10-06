package com.constructora_backend.service;

import com.constructora_backend.dto.response.CloudinaryFirmaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudinaryFirmaServiceTest {

    @Test
    @DisplayName("firmar() coincide con el ejemplo oficial de la documentación de Cloudinary")
    void firmar_ejemploOficialCloudinary() {
        Map<String, String> params = Map.of(
                "eager", "w_400,h_300,c_pad|w_260,h_200,c_crop",
                "public_id", "sample_image",
                "timestamp", "1315060510");

        String firma = CloudinaryFirmaService.firmar(params, "abcd");

        assertEquals("bfd09f95f331f558cbd1320e67aa8d488770583e", firma);
    }

    @Test
    @DisplayName("firmar() ordena los parámetros y omite los vacíos")
    void firmar_ordenaYOmiteVacios() {
        String conVacio = CloudinaryFirmaService.firmar(Map.of("timestamp", "100", "folder", "arquinova", "tags", ""), "s");
        String sinVacio = CloudinaryFirmaService.firmar(Map.of("folder", "arquinova", "timestamp", "100"), "s");
        assertEquals(sinVacio, conVacio);
    }

    @Test
    @DisplayName("generarFirma() devuelve datos coherentes y verificables")
    void generarFirma_configurado() {
        CloudinaryFirmaService service = new CloudinaryFirmaService("nube", "123", "secreto", "arquinova");

        CloudinaryFirmaResponseDTO dto = service.generarFirma();

        assertEquals("nube", dto.cloudName());
        assertEquals("123", dto.apiKey());
        assertEquals("arquinova", dto.folder());
        assertTrue(Math.abs(dto.timestamp() - java.time.Instant.now().getEpochSecond()) < 5);
        String esperada = CloudinaryFirmaService.firmar(
                Map.of("folder", "arquinova", "timestamp", String.valueOf(dto.timestamp())), "secreto");
        assertEquals(esperada, dto.signature());
    }

    @Test
    @DisplayName("Sin credenciales: estaConfigurado() es false y generarFirma() falla")
    void generarFirma_sinConfigurar() {
        CloudinaryFirmaService service = new CloudinaryFirmaService("", "", "", "arquinova");

        assertFalse(service.estaConfigurado());
        assertThrows(IllegalStateException.class, service::generarFirma);
    }
}
