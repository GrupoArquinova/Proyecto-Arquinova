package com.constructora_backend.dto;

import com.constructora_backend.dto.request.CasaModeloRequestDTO;
import com.constructora_backend.dto.request.EmpresaRequestDTO;
import com.constructora_backend.dto.request.MultimediaRequestDTO;
import com.constructora_backend.dto.request.ProyectoRequestDTO;
import com.constructora_backend.dto.request.UbicacionRequestDTO;
import com.constructora_backend.dto.request.ZonaComunImagenRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las URL que se guardan (las usa el sitio público como src, iframe o enlace) deben ser https; vacío o null en las opcionales. */
class UrlsHttpsTest {

    private static final String[] MALAS = {
            "javascript:alert(1)", "http://sitio.com/a.jpg", "data:image/png;base64,AAAA", "ftp://x.com/a", "//sitio.com/a.jpg",
            "https://sitio.com/con espacio.jpg", "HTTPS-no-es-url", "file:///etc/passwd"
    };
    private static final String BUENA = "https://res.cloudinary.com/arquinova/image/upload/v1/foto.jpg";

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static <T> boolean falla(T dto, String campo) {
        Set<ConstraintViolation<T>> errores = validator.validate(dto);
        return errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals(campo));
    }

    private MultimediaRequestDTO multimedia(String url, String urlEn) {
        MultimediaRequestDTO dto = new MultimediaRequestDTO();
        dto.setUrl(url);
        dto.setUrlEn(urlEn);
        return dto;
    }

    @Test
    @DisplayName("Multimedia: url y urlEn solo https; urlEn opcional")
    void multimediaUrls() {
        assertFalse(falla(multimedia(BUENA, null), "url"));
        assertFalse(falla(multimedia(BUENA, ""), "urlEn"));
        assertFalse(falla(multimedia(BUENA, BUENA), "urlEn"));
        for (String mala : MALAS) {
            assertTrue(falla(multimedia(mala, null), "url"), "url: " + mala);
            assertTrue(falla(multimedia(BUENA, mala), "urlEn"), "urlEn: " + mala);
        }
        assertTrue(falla(multimedia("", null), "url"), "la url del recurso sigue siendo obligatoria");
    }

    @Test
    @DisplayName("Proyecto: imagenUrl solo https; opcional")
    void proyectoImagen() {
        ProyectoRequestDTO dto = new ProyectoRequestDTO();
        dto.setImagenUrl(BUENA);
        assertFalse(falla(dto, "imagenUrl"));
        dto.setImagenUrl("");
        assertFalse(falla(dto, "imagenUrl"));
        dto.setImagenUrl(null);
        assertFalse(falla(dto, "imagenUrl"));
        for (String mala : MALAS) {
            dto.setImagenUrl(mala);
            assertTrue(falla(dto, "imagenUrl"), mala);
        }
    }

    @Test
    @DisplayName("Imagen de zona común: url solo https y obligatoria")
    void zonaComunImagen() {
        ZonaComunImagenRequestDTO dto = new ZonaComunImagenRequestDTO();
        dto.setImagenUrl(BUENA);
        assertFalse(falla(dto, "imagenUrl"));
        for (String mala : MALAS) {
            dto.setImagenUrl(mala);
            assertTrue(falla(dto, "imagenUrl"), mala);
        }
        dto.setImagenUrl("");
        assertTrue(falla(dto, "imagenUrl"));
    }

    @Test
    @DisplayName("Casa modelo: plano y tour virtual solo https; opcionales")
    void casaModelo() {
        CasaModeloRequestDTO dto = new CasaModeloRequestDTO();
        dto.setPlanoUrl(BUENA);
        dto.setTourVirtualUrl("https://tour.example.com/casa-a");
        assertFalse(falla(dto, "planoUrl"));
        assertFalse(falla(dto, "tourVirtualUrl"));
        dto.setPlanoUrl("");
        dto.setTourVirtualUrl(null);
        assertFalse(falla(dto, "planoUrl"));
        assertFalse(falla(dto, "tourVirtualUrl"));
        for (String mala : MALAS) {
            dto.setPlanoUrl(mala);
            dto.setTourVirtualUrl(mala);
            assertTrue(falla(dto, "planoUrl"), "plano: " + mala);
            assertTrue(falla(dto, "tourVirtualUrl"), "tour: " + mala);
        }
    }

    @Test
    @DisplayName("Ubicación: mapas, urbanismo, vista aérea, recorrido 360 y video solo https; opcionales")
    void ubicacion() {
        UbicacionRequestDTO dto = new UbicacionRequestDTO();
        String[] campos = {"googleMapsUrl", "urbanismoUrl", "urbanismoUrlEn", "vistaAereaUrl", "recorrido360Url", "videoComoLlegarUrl"};
        for (String mala : MALAS) {
            dto.setGoogleMapsUrl(mala);
            dto.setUrbanismoUrl(mala);
            dto.setUrbanismoUrlEn(mala);
            dto.setVistaAereaUrl(mala);
            dto.setRecorrido360Url(mala);
            dto.setVideoComoLlegarUrl(mala);
            for (String campo : campos) {
                assertTrue(falla(dto, campo), campo + ": " + mala);
            }
        }
        dto.setGoogleMapsUrl("https://www.google.com/maps/embed?pb=!1m18!1m12");
        dto.setUrbanismoUrl(BUENA);
        dto.setUrbanismoUrlEn("");
        dto.setVistaAereaUrl(null);
        dto.setRecorrido360Url(BUENA);
        dto.setVideoComoLlegarUrl("https://youtube.com/watch?v=abc");
        for (String campo : campos) {
            assertFalse(falla(dto, campo), campo);
        }
    }

    @Test
    @DisplayName("Empresa: logoUrl solo https; opcional")
    void empresaLogo() {
        EmpresaRequestDTO dto = new EmpresaRequestDTO();
        dto.setLogoUrl(BUENA);
        assertFalse(falla(dto, "logoUrl"));
        dto.setLogoUrl(null);
        assertFalse(falla(dto, "logoUrl"));
        dto.setLogoUrl("javascript:alert(1)");
        assertTrue(falla(dto, "logoUrl"));
    }
}
