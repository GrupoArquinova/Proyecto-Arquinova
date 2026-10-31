package com.constructora_backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/**
 * Usuario autenticado con el instante desde el cual valen sus tokens (columna token_valido_desde). Cuando se cambia la
 * contraseña, el rol o se desactiva la cuenta, ese instante avanza y los tokens emitidos antes dejan de aceptarse.
 */
public class UsuarioPrincipal extends User {

    private final LocalDateTime tokenValidoDesde;
    /** Nombre de la persona; viaja en el token para que el panel muestre quién tiene la sesión. */
    private final String nombreCompleto;

    public UsuarioPrincipal(String correo, String passwordHash, boolean activo,
                            Collection<? extends GrantedAuthority> autoridades, LocalDateTime tokenValidoDesde) {
        this(correo, passwordHash, activo, autoridades, tokenValidoDesde, null);
    }

    public UsuarioPrincipal(String correo, String passwordHash, boolean activo,
                            Collection<? extends GrantedAuthority> autoridades, LocalDateTime tokenValidoDesde,
                            String nombreCompleto) {
        super(correo, passwordHash, activo, true, true, true, autoridades);
        this.tokenValidoDesde = tokenValidoDesde;
        this.nombreCompleto = nombreCompleto;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public LocalDateTime getTokenValidoDesde() {
        return tokenValidoDesde;
    }

    /**
     * true si un token emitido en ese momento (milisegundos) todavía se acepta. La comparación es por segundos porque
     * el claim iat del JWT no guarda fracciones.
     */
    public boolean aceptaTokenEmitidoEn(long emisionMillis) {
        if (tokenValidoDesde == null) {
            return true;
        }
        long limite = tokenValidoDesde.atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
        return emisionMillis >= limite;
    }

    /** Aplica la regla a cualquier UserDetails: los que no traen ese dato (por ejemplo en pruebas) no se restringen. */
    public static boolean tokenAceptado(UserDetails usuario, long emisionMillis) {
        return !(usuario instanceof UsuarioPrincipal principal) || principal.aceptaTokenEmitidoEn(emisionMillis);
    }
}
