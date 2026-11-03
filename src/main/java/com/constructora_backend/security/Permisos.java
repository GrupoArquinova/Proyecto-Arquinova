package com.constructora_backend.security;

/**
 * Expresiones de @PreAuthorize y comprobaciones de rol del panel, en un solo lugar.
 * <ul>
 *   <li>ADMINISTRADOR: todo (usuarios, historial de cambios, métricas, reportes y exportaciones incluidos).</li>
 *   <li>EDITOR: crea, edita y borra el contenido del sitio (proyectos, ubicaciones, tipologías, amenidades, etapas, lotes,
 *       estados de lote, multimedia, contenido institucional y empresa). No ve solicitudes, usuarios, historial ni métricas.</li>
 *   <li>COMERCIAL: atiende las solicitudes de contacto (y administra sus estados), ve las métricas y lee proyectos y lotes
 *       para tener contexto. No edita contenido ni usuarios.</li>
 * </ul>
 * El rol se guarda en la base con o sin el prefijo ROLE_; al autenticar siempre se agrega (UserDetailsServiceImpl).
 */
public final class Permisos {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String COMERCIAL = "COMERCIAL";
    public static final String EDITOR = "EDITOR";

    /** Solo administradores. */
    public static final String SOLO_ADMIN = "hasRole('ADMINISTRADOR')";
    /** Escribir contenido del sitio (y leerlo completo, con borradores). */
    public static final String ADMIN_O_EDITOR = "hasAnyRole('ADMINISTRADOR','EDITOR')";
    /** Gestionar solicitudes de contacto y sus estados; ver métricas. */
    public static final String ADMIN_O_COMERCIAL = "hasAnyRole('ADMINISTRADOR','COMERCIAL')";
    /** Lectura interna de proyectos, lotes y su contexto (incluye lo no publicado). */
    public static final String LECTURA_INTERNA = "hasAnyRole('ADMINISTRADOR','EDITOR','COMERCIAL')";

    private Permisos() {
    }

    /** true si la autenticación (puede ser null o anónima) tiene alguno de esos roles (sin el prefijo ROLE_). */
    public static boolean tieneAlgunRol(org.springframework.security.core.Authentication autenticacion, String... roles) {
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return false;
        }
        for (org.springframework.security.core.GrantedAuthority autoridad : autenticacion.getAuthorities()) {
            String nombre = autoridad.getAuthority();
            for (String rol : roles) {
                if (("ROLE_" + rol).equals(nombre)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Quien edita contenido: ve también lo no publicado. */
    public static boolean puedeEditarContenido(org.springframework.security.core.Authentication autenticacion) {
        return tieneAlgunRol(autenticacion, ADMINISTRADOR, EDITOR);
    }

    /** Quien puede leer proyectos y lotes completos (borradores incluidos): administradores, editores y comerciales. */
    public static boolean puedeLeerTodo(org.springframework.security.core.Authentication autenticacion) {
        return tieneAlgunRol(autenticacion, ADMINISTRADOR, EDITOR, COMERCIAL);
    }
}
