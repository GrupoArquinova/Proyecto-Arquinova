package com.constructora_backend.controller;

import com.constructora_backend.dto.request.UsuarioRequestDTO;
import com.constructora_backend.dto.request.UsuarioUpdateDTO;
import com.constructora_backend.dto.response.UsuarioResponseDTO;
import com.constructora_backend.security.JwtUtils;
import com.constructora_backend.security.UserDetailsServiceImpl;
import com.constructora_backend.service.UsuarioService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private UsuarioResponseDTO usuarioResponseDTO;

    @BeforeEach
    void setUp() {
        usuarioResponseDTO = new UsuarioResponseDTO();
        usuarioResponseDTO.setId(1L);
        usuarioResponseDTO.setNombreCompleto("Admin Test");
        usuarioResponseDTO.setCorreo("admin@test.com");
        usuarioResponseDTO.setRolId(1L);
        usuarioResponseDTO.setActivo(true);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/usuarios debe retornar lista de DTOs")
    void listar_exito() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of(usuarioResponseDTO));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Admin Test"))
                .andExpect(jsonPath("$[0].correo").value("admin@test.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/usuarios/{id} debe retornar DTO de usuario si existe")
    void obtenerPorId_exito() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(Optional.of(usuarioResponseDTO));

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreCompleto").value("Admin Test"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/usuarios debe recibir UsuarioRequestDTO y retornar 201 Created")
    void crear_exito() throws Exception {
        UsuarioRequestDTO requestDTO = new UsuarioRequestDTO();
        requestDTO.setNombreCompleto("Nuevo Usuario");
        requestDTO.setCorreo("nuevo@test.com");
        requestDTO.setPassword("Password123!");
        requestDTO.setRolId(1L);

        when(usuarioService.guardar(any(UsuarioRequestDTO.class))).thenReturn(usuarioResponseDTO);

        mockMvc.perform(post("/api/usuarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/usuarios/{id} debe recibir UsuarioUpdateDTO y retornar DTO actualizado")
    void actualizar_exito() throws Exception {
        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setNombreCompleto("Admin Actualizado");

        when(usuarioService.actualizar(eq(1L), any(UsuarioUpdateDTO.class))).thenReturn(usuarioResponseDTO);

        mockMvc.perform(put("/api/usuarios/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/usuarios/{id} debe retornar 204 No Content")
    void eliminar_exito() throws Exception {
        doNothing().when(usuarioService).eliminar(1L);

        mockMvc.perform(delete("/api/usuarios/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
