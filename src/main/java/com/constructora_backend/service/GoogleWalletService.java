package com.constructora_backend.service;

import com.constructora_backend.exception.ServicioNoDisponibleException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tarjeta digital "Grupo Arquinova" para Google Wallet (Android): lleva el QR del sitio web, el enlace y el WhatsApp de la
 * empresa. El enlace "Guardar en Google Wallet" es un JWT firmado con la cuenta de servicio, así que la llave privada
 * nunca va en el repositorio ni dentro del .jar: se entrega por variable de entorno (GOOGLE_WALLET_CREDENCIALES, el JSON
 * tal cual o en base64) o por la ruta de un archivo (GOOGLE_WALLET_CREDENCIALES_ARCHIVO). Sin ninguna de las dos, la
 * función queda apagada y el sitio responde 503 en lugar de fallar con un error interno.
 */
@Service
@Slf4j
public class GoogleWalletService {

    /** Google rechaza los enlaces "guardar" con un JWT de más de este largo (caracteres). */
    static final int MAX_LARGO_ENLACE_JWT = 1800;

    @Value("${google.wallet.issuer-id:3388000000023232718}")
    private String issuerId;

    @Value("${google.wallet.class-id:3388000000023232718.arquinova_pass}")
    private String classId;

    /** JSON de la cuenta de servicio, tal cual o en base64 (GOOGLE_WALLET_CREDENCIALES). */
    @Value("${google.wallet.credenciales:}")
    private String credencialesTexto;

    /** Ruta de un archivo con el JSON de la cuenta de servicio, para el equipo local (GOOGLE_WALLET_CREDENCIALES_ARCHIVO). */
    @Value("${google.wallet.credenciales-archivo:}")
    private String credencialesArchivo;

    @Value("${google.wallet.logo-url:}")
    private String logoUrl;

    @Value("${google.wallet.color:#2c5953}")
    private String color;

    /** Dirección pública del sitio (APP_SITIO_URL): es lo que codifica el QR de la tarjeta. */
    @Value("${app.sitio-url:}")
    private String sitioUrl;

    /** Si no hay APP_SITIO_URL se usa el primer origen permitido (en local, http://localhost:4200). */
    @Value("${cors.allowed-origins:}")
    private String origenesPermitidos;

    @Value("${app.contacto.whatsapp:}")
    private String whatsapp;

    private volatile ServiceAccountCredentials credenciales;

    public GoogleWalletService() {
    }

    /** Para pruebas: credenciales ya construidas. */
    GoogleWalletService(ServiceAccountCredentials credenciales, String issuerId, String classId, String sitioUrl) {
        this.credenciales = credenciales;
        this.issuerId = issuerId;
        this.classId = classId;
        this.sitioUrl = sitioUrl;
        this.credencialesTexto = "";
        this.credencialesArchivo = "";
        this.logoUrl = "";
        this.color = "#2c5953";
        this.origenesPermitidos = "";
        this.whatsapp = "";
    }

    /** true si hay credenciales válidas y una dirección del sitio para el QR. */
    public boolean disponible() {
        try {
            return credenciales() != null && !direccionDelSitio().isBlank();
        } catch (ServicioNoDisponibleException e) {
            return false;
        }
    }

    /** Enlace "Guardar en Google Wallet" de la tarjeta de Arquinova para ese correo (el mismo correo siempre da la misma tarjeta, sin duplicados). */
    public String generarEnlace(String correo) {
        ServiceAccountCredentials cuenta = credenciales();
        String sitio = direccionDelSitio();
        if (cuenta == null || sitio.isBlank()) {
            throw new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora.");
        }

        Map<String, Object> clase = new LinkedHashMap<>();
        clase.put("id", classId);

        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("id", issuerId + ".tarjeta_" + huella(correo));
        objeto.put("classId", classId);
        objeto.put("state", "ACTIVE");
        objeto.put("cardTitle", texto("Grupo Arquinova"));
        objeto.put("header", texto("Arquitectura e ingeniería"));
        objeto.put("hexBackgroundColor", color);
        if (logoUrl != null && !logoUrl.isBlank()) {
            objeto.put("logo", Map.of("sourceUri", Map.of("uri", logoUrl.trim())));
        }
        objeto.put("barcode", Map.of("type", "QR_CODE", "value", sitio));
        objeto.put("linksModuleData", Map.of("uris", List.of(
                Map.of("id", "sitio", "uri", sitio, "description", "Visitar nuestro sitio"))));
        if (whatsapp != null && !whatsapp.isBlank()) {
            objeto.put("textModulesData", List.of(Map.of("id", "whatsapp", "header", "WhatsApp", "body", whatsapp.trim())));
        }

        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("genericClasses", List.of(clase));
        carga.put("genericObjects", List.of(objeto));

        Map<String, Object> reclamos = new LinkedHashMap<>();
        reclamos.put("iss", cuenta.getClientEmail());
        reclamos.put("aud", "google");
        reclamos.put("typ", "savetowallet");
        reclamos.put("payload", carga);

        String jwt = Jwts.builder()
                .claims(reclamos)
                .signWith(cuenta.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
        if (jwt.length() > MAX_LARGO_ENLACE_JWT) {
            // Google lo rechazaría con "algo salió mal": mejor avisar con claridad que entregar un enlace que no sirve
            log.error("El JWT de Google Wallet mide {} caracteres y el máximo es {}: acorta los textos o la URL del logo.", jwt.length(), MAX_LARGO_ENLACE_JWT);
            throw new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora.");
        }
        return "https://pay.google.com/gp/v/save/" + jwt;
    }

    private static Map<String, Object> texto(String valor) {
        return Map.of("defaultValue", Map.of("language", "es-CO", "value", valor));
    }

    /** Identificador estable por correo (sin guardar el correo en Google): solo letras y números, que es lo que admite Wallet. */
    private static String huella(String correo) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(correo.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String direccionDelSitio() {
        String direccion = sitioUrl == null ? "" : sitioUrl.trim();
        if (direccion.isBlank() && origenesPermitidos != null) {
            direccion = List.of(origenesPermitidos.split(",")).stream().map(String::trim).filter(o -> !o.isBlank()).findFirst().orElse("");
        }
        return direccion.replaceAll("/+$", "");
    }

    private ServiceAccountCredentials credenciales() {
        ServiceAccountCredentials actual = credenciales;
        if (actual != null) return actual;
        synchronized (this) {
            if (credenciales != null) return credenciales;
            byte[] json = leerJson();
            if (json == null) return null;
            try {
                credenciales = ServiceAccountCredentials.fromStream(new ByteArrayInputStream(json));
                return credenciales;
            } catch (IOException | RuntimeException e) {
                log.error("Las credenciales de Google Wallet no son válidas: {}", e.getMessage());
                throw new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora.");
            }
        }
    }

    private byte[] leerJson() {
        String texto = credencialesTexto == null ? "" : credencialesTexto.trim();
        if (!texto.isEmpty()) {
            if (texto.startsWith("{")) return texto.getBytes(StandardCharsets.UTF_8);
            try {
                return Base64.getMimeDecoder().decode(texto);
            } catch (IllegalArgumentException e) {
                log.error("GOOGLE_WALLET_CREDENCIALES no es un JSON ni está en base64 válido.");
                throw new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora.");
            }
        }
        if (credencialesArchivo != null && !credencialesArchivo.isBlank()) {
            try {
                return Files.readAllBytes(Path.of(credencialesArchivo.trim()));
            } catch (IOException | RuntimeException e) {
                log.error("No se pudo leer el archivo de credenciales de Google Wallet ({}).", e.getClass().getSimpleName());
                throw new ServicioNoDisponibleException("La tarjeta digital no está disponible por ahora.");
            }
        }
        return null;
    }
}
