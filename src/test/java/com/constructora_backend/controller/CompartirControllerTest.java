package com.constructora_backend.controller;

import com.constructora_backend.config.SecurityConfig;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.repository.ProyectoRepository;
import com.constructora_backend.security.JwtFilter;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.CompartirService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Etiquetas para compartir un proyecto: públicas (seguridad real), HTML y con caché en el CDN. */
@WebMvcTest(CompartirController.class)
@Import({SecurityConfig.class, JwtFilter.class, CompartirService.class})
@TestPropertySource(properties = "app.sitio-url=https://www.arquinova.test")
class CompartirControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProyectoRepository proyectoRepository;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private void proyecto(long id, boolean publicado) {
        Proyecto p = new Proyecto();
        p.setId(id);
        p.setNombre("Villas El Encanto");
        p.setDescripcion("Lotes campestres");
        p.setPublicado(publicado);
        p.setActivo(true);
        when(proyectoRepository.findById(id)).thenReturn(Optional.of(p));
    }

    @Test
    @DisplayName("Público, text/html y caché de 1 hora en el CDN, con las etiquetas del proyecto")
    void publicoConEtiquetas() throws Exception {
        proyecto(6, true);

        mockMvc.perform(get("/api/publico/compartir/proyectos/6"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.startsWith("text/html")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("s-maxage=3600")))
                .andExpect(content().string(Matchers.containsString("<meta property=\"og:title\" content=\"Villas El Encanto | Grupo Arquinova\">")))
                .andExpect(content().string(Matchers.containsString("<meta property=\"og:url\" content=\"https://www.arquinova.test/proyectos/6\">")));
    }

    @Test
    @DisplayName("Un proyecto sin publicar o inexistente es 404")
    void noPublicadoEs404() throws Exception {
        proyecto(7, false);
        when(proyectoRepository.findById(8L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/publico/compartir/proyectos/7")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/publico/compartir/proyectos/8")).andExpect(status().isNotFound());
    }
}
