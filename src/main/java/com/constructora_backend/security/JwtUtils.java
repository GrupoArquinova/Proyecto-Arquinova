package com.constructora_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
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

    /** Claim con el momento (milisegundos) en que la persona inició sesión con su contraseña; las renovaciones lo conservan. */
    static final String CLAIM_SESION_INICIO = "sesionInicio";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    /** Vida máxima absoluta de una sesión, por más que se renueve (JWT_SESION_MAXIMA_HORAS). */
    @Value("${jwt.sesion-maxima-horas:12}")
    private long sesionMaximaHoras = 12;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(Authentication authentication){
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        long ahora = System.currentTimeMillis();
        return construirToken(userPrincipal.getUsername(), nombreDe(userPrincipal), roles, ahora, ahora);
    }

    /**
     * Token nuevo para una sesión que se renueva. Los roles se leen de nuevo de la base (los que traiga el UserDetails),
     * así que un cambio de rol o una cuenta desactivada se nota en la siguiente renovación. Conserva el inicio de la
     * sesión original para que el límite absoluto de duración no se pueda estirar renovando.
     */
    public String generarTokenRenovado(UserDetails usuario, long sesionInicioMillis) {
        List<String> roles = usuario.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return construirToken(usuario.getUsername(), nombreDe(usuario), roles, System.currentTimeMillis(), sesionInicioMillis);
    }

    /** Nombre de la persona si el usuario lo trae (los UserDetails genéricos, por ejemplo en pruebas, no). */
    private static String nombreDe(UserDetails usuario) {
        return usuario instanceof UsuarioPrincipal principal ? principal.getNombreCompleto() : null;
    }

    private String construirToken(String correo, String nombre, Collection<String> roles, long ahoraMillis, long sesionInicioMillis) {
        Date ahora = new Date(ahoraMillis);
        JwtBuilder constructor = Jwts.builder()
                .subject(correo)
                // Solo para que la interfaz sepa qué mostrar; el backend siempre decide con los roles de la base de datos
                .claim("roles", roles)
                .claim(CLAIM_SESION_INICIO, sesionInicioMillis);
        if (nombre != null && !nombre.isBlank()) {
            constructor.claim("nombre", nombre.trim());     // para mostrar quién tiene la sesión; no se usa para decidir permisos
        }
        return constructor
                .issuedAt(ahora)
                .expiration(new Date(ahoraMillis + jwtExpirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims leer(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Momento (en milisegundos) en que vence un token firmado y vigente. */
    public long obtenerExpiracionMillis(String token) {
        return leer(token).getExpiration().getTime();
    }

    /** Momento (en milisegundos, con precisión de segundos) en que se emitió el token. */
    public long obtenerEmisionMillis(String token) {
        return leer(token).getIssuedAt().getTime();
    }

    /** Momento en que empezó la sesión; los tokens anteriores a este claim usan su propia emisión. */
    public long obtenerSesionInicioMillis(String token) {
        Claims claims = leer(token);
        Object inicio = claims.get(CLAIM_SESION_INICIO);
        if (inicio instanceof Number numero) {
            return numero.longValue();
        }
        return claims.getIssuedAt().getTime();
    }

    /** true si la sesión que empezó en ese momento ya superó su vida máxima y no se puede renovar. */
    public boolean sesionAgotada(long sesionInicioMillis) {
        return System.currentTimeMillis() - sesionInicioMillis > sesionMaximaHoras * 3_600_000L;
    }

    public String obtenerCorreoDelToken(String token){
        return leer(token).getSubject();
    }

    public boolean validarToken(String authToken) {
        try {
            leer(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }
}
