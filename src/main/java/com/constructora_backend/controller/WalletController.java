package com.constructora_backend.controller;

import com.constructora_backend.dto.request.PaseWalletRequestDTO;
import com.constructora_backend.exception.RateLimitExceededException;
import com.constructora_backend.exception.ServicioNoDisponibleException;
import com.constructora_backend.service.EmailService;
import com.constructora_backend.service.GoogleWalletService;
import com.constructora_backend.service.LimitadorPasesWallet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Tarjeta digital de Grupo Arquinova para Google Wallet: público, con correo validado y límites por IP, por dirección y global. */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Google Wallet", description = "Tarjeta digital de la empresa con el QR del sitio")
public class WalletController {

    private final GoogleWalletService googleWalletService;
    private final EmailService emailService;
    private final LimitadorPasesWallet limitador;

    @PostMapping("/enviar-pase")
    @Operation(summary = "Envía por correo el enlace para guardar la tarjeta de Arquinova en Google Wallet")
    public ResponseEntity<Map<String, String>> enviarPaseAlCorreo(@Valid @RequestBody PaseWalletRequestDTO solicitud) {
        // Antes de gastar un cupo: si la función no está configurada se avisa, en vez de decir "enviado" y no mandar nada
        if (!googleWalletService.disponible()) {
            throw new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora.");
        }
        String correo = solicitud.getEmail().trim();

        switch (limitador.permitir(correo)) {
            case DIRECCION_RECIENTE -> throw new RateLimitExceededException(
                    "Ya enviamos la tarjeta a ese correo hace poco. Revisa tu bandeja (y el spam) o inténtalo de nuevo en una hora.");
            case TOPE_GLOBAL -> throw new RateLimitExceededException(
                    "Hay muchas solicitudes en este momento. Inténtalo de nuevo más tarde.");
            case PERMITIDO -> { }
        }

        try {
            emailService.enviarCorreoConBotonWallet(correo, googleWalletService.generarEnlace(correo));
        } catch (RuntimeException e) {
            limitador.liberar(correo);
            throw e;
        }
        return ResponseEntity.ok(Map.of("mensaje", "Tarjeta enviada con éxito"));
    }
}
