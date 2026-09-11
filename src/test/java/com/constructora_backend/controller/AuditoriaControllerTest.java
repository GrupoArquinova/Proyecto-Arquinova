package com.constructora_backend.controller;

import com.constructora_backend.dto.request.AuditoriaRequestDTO;
import com.constructora_backend.dto.response.AuditoriaResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditoriaService auditoriaService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private AuditoriaResponseDTO auditoriaResponseDTO;

    @BeforeEach
    void setUp() {
        auditoriaResponseDTO = new AuditoriaResponseDTO();
        auditoriaResponseDTO.setId(1L);
        auditoriaResponseDTO.setAccion("ACTUALIZAR_EMPRESA");
        auditoriaResponseDTO.setEntidad("EMPRESAS");
        auditoriaResponseDTO.setEntidadId(2L);
        auditoriaResponseDTO.setUsuarioId(1L);
        auditoriaResponseDTO.setUsuarioNombre("Admin");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/auditoria debe retornar lista de auditorías")
    void listar_exito() throws Exception {
        when(auditoriaService.listarTodas()).thenReturn(List.of(auditoriaResponseDTO));

        mockMvc.perform(get("/api/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accion").value("ACTUALIZAR_EMPRESA"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/auditoria/{id} debe retornar auditoría por ID")
    void obtenerPorId_exito() throws Exception {
        when(auditoriaService.obtenerPorId(1L)).thenReturn(Optional.of(auditoriaResponseDTO));

        mockMvc.perform(get("/api/auditoria/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.entidad").value("EMPRESAS"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/auditoria/entidad/{entidad}/{id} debe retornar lista filtrada")
    void obtenerPorEntidad_exito() throws Exception {
        when(auditoriaService.obtenerPorEntidad("EMPRESAS", 2L)).thenReturn(List.of(auditoriaResponseDTO));

        mockMvc.perform(get("/api/auditoria/entidad/EMPRESAS/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/auditoria debe registrar una auditoría y retornar 201 Created")
    void registrar_exito() throws Exception {
        AuditoriaRequestDTO requestDTO = new AuditoriaRequestDTO();
        requestDTO.setAccion("CREAR_USUARIO");
        requestDTO.setEntidad("USUARIOS");
        requestDTO.setEntidadId(10L);

        when(auditoriaService.registrar(any(AuditoriaRequestDTO.class))).thenReturn(auditoriaResponseDTO);

        mockMvc.perform(post("/api/auditoria")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }
}
