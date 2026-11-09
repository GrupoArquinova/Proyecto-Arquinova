package com.constructora_backend.service;

import com.constructora_backend.exception.ServicioNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La tarjeta de Google Wallet: contenido del pase, firma, tamaño del enlace y entrega de las credenciales sin archivos en el repositorio. */
class GoogleWalletServiceTest {

    private static final String PREFIJO = "https://pay.google.com/gp/v/save/";
    private static final String ISSUER = "3388000000023232718";
    private static final String CLASE = ISSUER + ".arquinova_pass";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static KeyPair llaves;
    private static ServiceAccountCredentials cuenta;

    @BeforeAll
    static void crearCuenta() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        llaves = generador.generateKeyPair();
        cuenta = ServiceAccountCredentials.newBuilder()
                .setClientEmail("arquinova-wallet@arquinova-wallet.iam.gserviceaccount.com")
                .setPrivateKey(llaves.getPrivate())
                .setPrivateKeyId("prueba")
                .build();
    }

    private static GoogleWalletService servicio(String sitio) {
        return new GoogleWalletService(cuenta, ISSUER, CLASE, sitio);
    }

    private static JsonNode cuerpo(String enlace) throws Exception {
        assertThat(enlace).startsWith(PREFIJO);
        String jwt = enlace.substring(PREFIJO.length());
        String[] partes = jwt.split("\\.");
        assertThat(partes).hasSize(3);
        return JSON.readTree(Base64.getUrlDecoder().decode(partes[1]));
    }

    @Test
    @DisplayName("La tarjeta es de Grupo Arquinova y el QR lleva la dirección del sitio")
    void elQrLlevaLaPagina() throws Exception {
        JsonNode jwt = cuerpo(servicio("https://arquinova.vercel.app/").generarEnlace("cliente@correo.com"));

        assertThat(jwt.path("iss").asText()).isEqualTo("arquinova-wallet@arquinova-wallet.iam.gserviceaccount.com");
        assertThat(jwt.path("aud").asText()).isEqualTo("google");
        assertThat(jwt.path("typ").asText()).isEqualTo("savetowallet");

        JsonNode objeto = jwt.path("payload").path("genericObjects").get(0);
        assertThat(objeto.path("classId").asText()).isEqualTo(CLASE);
        assertThat(jwt.path("payload").path("genericClasses").get(0).path("id").asText()).isEqualTo(CLASE);
        assertThat(objeto.path("state").asText()).isEqualTo("ACTIVE");
        assertThat(objeto.path("cardTitle").path("defaultValue").path("value").asText()).isEqualTo("Grupo Arquinova");
        // Sin estos dos campos Google responde "algo salió mal" al guardar un pase genérico
        assertThat(objeto.path("header").path("defaultValue").path("value").asText()).isNotBlank();
        assertThat(objeto.path("barcode").path("type").asText()).isEqualTo("QR_CODE");
        assertThat(objeto.path("barcode").path("value").asText()).isEqualTo("https://arquinova.vercel.app");
        assertThat(objeto.path("linksModuleData").path("uris").get(0).path("uri").asText()).isEqualTo("https://arquinova.vercel.app");
        // No es un pase por cliente ni por lote
        assertThat(objeto.toString()).doesNotContain("ARQ-LOTE").doesNotContain("Cliente Arquinova").doesNotContain("cliente@correo.com");
    }

    @Test
    @DisplayName("Va firmado RS256 con la llave de la cuenta de servicio")
    void vaFirmado() throws Exception {
        String enlace = servicio("https://arquinova.vercel.app").generarEnlace("cliente@correo.com");
        io.jsonwebtoken.Jwts.parser().verifyWith((RSAPublicKey) llaves.getPublic()).build()
                .parseSignedClaims(enlace.substring(PREFIJO.length()));
    }

    @Test
    @DisplayName("El mismo correo da siempre la misma tarjeta y otro correo, otra; el identificador no deja ver el correo")
    void identificadorEstable() throws Exception {
        GoogleWalletService servicio = servicio("https://arquinova.vercel.app");
        String a1 = cuerpo(servicio.generarEnlace("Ana@Correo.com")).path("payload").path("genericObjects").get(0).path("id").asText();
        String a2 = cuerpo(servicio.generarEnlace(" ana@correo.com ")).path("payload").path("genericObjects").get(0).path("id").asText();
        String b = cuerpo(servicio.generarEnlace("beto@correo.com")).path("payload").path("genericObjects").get(0).path("id").asText();

        assertThat(a1).isEqualTo(a2).isNotEqualTo(b).startsWith(ISSUER + ".").doesNotContain("ana").doesNotContain("@");
        assertThat(a1).matches("^[0-9]+\\.[A-Za-z0-9_]+$");
    }

    @Test
    @DisplayName("El enlace no pasa de 1800 caracteres (Google lo rechaza) ni con el logo, el WhatsApp y un sitio largo")
    void noSuperaElLimiteDeGoogle() {
        GoogleWalletService servicio = servicio("https://www.arquinova-constructora.com.co");
        ReflectionTestUtils.setField(servicio, "logoUrl", "https://res.cloudinary.com/dn3s7utod/image/upload/v1790258444/logo-Arquinova_dgkosg.jpg");
        ReflectionTestUtils.setField(servicio, "whatsapp", "+57 316 865 3715");

        String enlace = servicio.generarEnlace("una.persona@dominio-de-prueba.com.co");

        assertThat(enlace.length() - PREFIJO.length()).isLessThanOrEqualTo(GoogleWalletService.MAX_LARGO_ENLACE_JWT);
    }

    @Test
    @DisplayName("Si el enlace saldría más largo de lo que acepta Google, se avisa en vez de entregar uno que no sirve")
    void rechazaUnEnlaceDemasiadoLargo() {
        GoogleWalletService servicio = servicio("https://arquinova.vercel.app");
        ReflectionTestUtils.setField(servicio, "logoUrl", "https://cdn.test/" + "x".repeat(1500) + ".png");

        assertThatThrownBy(() -> servicio.generarEnlace("cliente@correo.com")).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Sin APP_SITIO_URL usa el primer origen permitido; sin ninguno la función no está disponible")
    void direccionDelSitio() throws Exception {
        GoogleWalletService servicio = servicio("");
        assertThat(servicio.disponible()).isFalse();
        assertThatThrownBy(() -> servicio.generarEnlace("a@b.co")).isInstanceOf(ServicioNoDisponibleException.class);

        ReflectionTestUtils.setField(servicio, "origenesPermitidos", "http://localhost:4200, https://otro.test");
        assertThat(servicio.disponible()).isTrue();
        assertThat(cuerpo(servicio.generarEnlace("a@b.co")).path("payload").path("genericObjects").get(0)
                .path("barcode").path("value").asText()).isEqualTo("http://localhost:4200");
    }

    @Test
    @DisplayName("Sin credenciales la función está apagada y no se rompe")
    void sinCredenciales() {
        GoogleWalletService servicio = new GoogleWalletService();
        ReflectionTestUtils.setField(servicio, "sitioUrl", "https://arquinova.vercel.app");

        assertThat(servicio.disponible()).isFalse();
        assertThatThrownBy(() -> servicio.generarEnlace("a@b.co")).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Las credenciales llegan por variable (JSON o base64) o por archivo, nunca desde el .jar")
    void credencialesPorVariableOArchivo(@org.junit.jupiter.api.io.TempDir Path carpeta) throws Exception {
        String json = jsonDeCuenta();

        for (String valor : new String[]{json, Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8))}) {
            GoogleWalletService servicio = new GoogleWalletService();
            ReflectionTestUtils.setField(servicio, "sitioUrl", "https://arquinova.vercel.app");
            ReflectionTestUtils.setField(servicio, "credencialesTexto", valor);
            assertThat(servicio.disponible()).isTrue();
            assertThat(servicio.generarEnlace("a@b.co")).startsWith(PREFIJO);
        }

        Path archivo = carpeta.resolve("cuenta.json");
        Files.writeString(archivo, json);
        GoogleWalletService porArchivo = new GoogleWalletService();
        ReflectionTestUtils.setField(porArchivo, "sitioUrl", "https://arquinova.vercel.app");
        ReflectionTestUtils.setField(porArchivo, "credencialesArchivo", archivo.toString());
        assertThat(porArchivo.disponible()).isTrue();
    }

    @Test
    @DisplayName("Credenciales dañadas o archivo inexistente: la función queda apagada, sin mostrar detalles")
    void credencialesInvalidas() {
        for (String[] caso : new String[][]{{"esto no es json ni base64 !!", ""}, {"", "no/existe.json"}, {"e30=", ""}}) {
            GoogleWalletService servicio = new GoogleWalletService();
            ReflectionTestUtils.setField(servicio, "sitioUrl", "https://arquinova.vercel.app");
            ReflectionTestUtils.setField(servicio, "credencialesTexto", caso[0]);
            ReflectionTestUtils.setField(servicio, "credencialesArchivo", caso[1]);
            assertThat(servicio.disponible()).isFalse();
        }
    }

    @Test
    @DisplayName("Ninguna llave ni credencial está dentro de src/main/resources")
    void sinLlavesEnElProyecto() throws Exception {
        try (var archivos = Files.walk(Path.of("src/main/resources"))) {
            assertThat(archivos.filter(Files::isRegularFile)
                    .filter(a -> a.getFileName().toString().endsWith(".json"))
                    .filter(a -> {
                        try { return Files.readString(a).contains("PRIVATE KEY"); } catch (Exception e) { return false; }
                    })).isEmpty();
        }
    }

    private static String jsonDeCuenta() {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(llaves.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        return "{\"type\":\"service_account\",\"project_id\":\"prueba\",\"private_key_id\":\"abc\","
                + "\"private_key\":" + JSON.valueToTree(pem) + ","
                + "\"client_email\":\"arquinova-wallet@arquinova-wallet.iam.gserviceaccount.com\",\"client_id\":\"1\"}";
    }
}
