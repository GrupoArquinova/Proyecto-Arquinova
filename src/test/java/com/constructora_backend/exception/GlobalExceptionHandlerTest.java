package com.constructora_backend.exception;

import com.constructora_backend.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("Manejar ResourceNotFoundException debe retornar 404 Not Found")
    void handleResourceNotFound_exito() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Recurso no encontrado");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleResourceNotFound(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Recurso no encontrado", response.getBody().getMensaje());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    @DisplayName("Manejar BadRequestException debe retornar 400 Bad Request")
    void handleBadRequest_exito() {
        BadRequestException ex = new BadRequestException("Solicitud inválida");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBadRequest(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Solicitud inválida", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Manejar DuplicateResourceException debe retornar 409 Conflict")
    void handleDuplicateResource_exito() {
        DuplicateResourceException ex = new DuplicateResourceException("El correo ya existe");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleDuplicateResource(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("El correo ya existe", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Manejar MethodArgumentNotValidException debe retornar 400 Bad Request con detalles por campo")
    void handleValidationExceptions_exito() {
        FieldError fieldError = new FieldError("usuarioDTO", "correo", "El correo es obligatorio");
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationExceptions(methodArgumentNotValidException, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertNotNull(response.getBody().getDetalles());
        assertEquals("El correo es obligatorio", response.getBody().getDetalles().get("correo"));
    }

    @Test
    @DisplayName("Manejar BadCredentialsException debe retornar 401 Unauthorized")
    void handleBadCredentials_exito() {
        BadCredentialsException ex = new BadCredentialsException("Credenciales incorrectas");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBadCredentials(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getStatus());
    }

    @Test
    @DisplayName("Manejar Exception genérica debe retornar 500 Internal Server Error")
    void handleGlobalException_exito() {
        Exception ex = new Exception("Error inesperado");

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGlobalException(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Ocurrió un error interno en el servidor. Por favor intente más tarde.", response.getBody().getMensaje());
    }
}
