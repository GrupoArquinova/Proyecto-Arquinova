package com.constructora_backend.service;

import com.constructora_backend.dto.response.CloudinaryFirmaResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Genera firmas para subidas directas (navegador -> Cloudinary).
 *
 * Algoritmo de Cloudinary: ordenar los parámetros alfabéticamente, unirlos como
 * "clave=valor&clave=valor", concatenar el API secret al final y calcular SHA-1.
 * Cloudinary rechaza firmas con más de 1 hora de antigüedad.
 */
@Service
public class CloudinaryFirmaService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;

    public CloudinaryFirmaService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.folder:arquinova}") String folder) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = folder;
    }

    /** true si las tres credenciales de Cloudinary están configuradas. */
    public boolean estaConfigurado() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    public CloudinaryFirmaResponseDTO generarFirma() {
        if (!estaConfigurado()) {
            throw new IllegalStateException("Cloudinary no está configurado (CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET)");
        }
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> params = new TreeMap<>();
        params.put("timestamp", String.valueOf(timestamp));
        if (!folder.isBlank()) {
            params.put("folder", folder);
        }
        String signature = firmar(params, apiSecret);
        return new CloudinaryFirmaResponseDTO(cloudName, apiKey, timestamp, folder, signature);
    }

    /**
     * Firma un conjunto de parámetros según el algoritmo de Cloudinary (SHA-1, hex).
     * Los parámetros vacíos se omiten, igual que hace Cloudinary.
     */
    static String firmar(Map<String, String> params, String apiSecret) {
        String cadena = new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest((cadena + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no disponible en la JVM", e);
        }
    }
}
