package com.constructora_backend.controller;

import com.constructora_backend.dto.request.EmpresaRequestDTO;
import com.constructora_backend.dto.response.EmpresaResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.EmpresaService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpresaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmpresaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmpresaService empresaService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private EmpresaResponseDTO empresaResponseDTO;

    @BeforeEach
    void setUp() {
        empresaResponseDTO = new EmpresaResponseDTO();
        empresaResponseDTO.setId(1L);
        empresaResponseDTO.setNombre("Constructora Conclave");
        empresaResponseDTO.setNit("900123456-1");
        empresaResponseDTO.setCorreoComercial("contacto@conclave.com");
        empresaResponseDTO.setActivo(true);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/empresas debe retornar lista de empresas")
    void listar_exito() throws Exception {
        when(empresaService.listarTodas()).thenReturn(List.of(empresaResponseDTO));

        mockMvc.perform(get("/api/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Constructora Conclave"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/empresas/{id} debe retornar empresa por ID")
    void obtenerPorId_exito() throws Exception {
        when(empresaService.obtenerPorId(1L)).thenReturn(Optional.of(empresaResponseDTO));

        mockMvc.perform(get("/api/empresas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nit").value("900123456-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/empresas debe registrar empresa y retornar 201 Created")
    void crear_exito() throws Exception {
        EmpresaRequestDTO requestDTO = new EmpresaRequestDTO();
        requestDTO.setNombre("Nueva Constructora");
        requestDTO.setNit("900999888-7");

        when(empresaService.guardar(any(EmpresaRequestDTO.class))).thenReturn(empresaResponseDTO);

        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/empresas/{id} debe actualizar empresa y retornar DTO")
    void actualizar_exito() throws Exception {
        EmpresaRequestDTO requestDTO = new EmpresaRequestDTO();
        requestDTO.setNombre("Constructora Actualizada");

        when(empresaService.actualizar(eq(1L), any(EmpresaRequestDTO.class))).thenReturn(empresaResponseDTO);

        mockMvc.perform(put("/api/empresas/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/empresas/{id} debe desactivar empresa y retornar 204 No Content")
    void desactivar_exito() throws Exception {
        doNothing().when(empresaService).desactivar(1L);

        mockMvc.perform(delete("/api/empresas/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
