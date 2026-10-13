package com.constructora_backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(Authentication authentication){
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return construirToken(userPrincipal.getUsername(), roles);
    }

    /**
     * Token nuevo para una sesión que se renueva. Los roles se leen de nuevo de la base (los que traiga el UserDetails),
     * así que un cambio de rol o una cuenta desactivada se nota en la siguiente renovación.
     */
    public String generarTokenRenovado(UserDetails usuario) {
        List<String> roles = usuario.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return construirToken(usuario.getUsername(), roles);
    }

    private String construirToken(String correo, Collection<String> roles) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(correo)
                // Solo para que la interfaz sepa qué mostrar; el backend siempre decide con los roles de la base de datos
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** Momento (en milisegundos) en que vence un token firmado y vigente. */
    public long obtenerExpiracionMillis(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .getTime();
    }

    public String obtenerCorreoDelToken(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validarToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }
}
