package com.constructora_backend.config;

import com.constructora_backend.security.JwtFilter;
import com.constructora_backend.security.MetricasTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final LimiteTamanoFilter limiteTamanoFilter;
    private final MetricasTokenFilter metricasTokenFilter;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    /** Swagger y la documentación OpenAPI: cerrados por defecto; solo se abren en desarrollo (APP_DOCS_PUBLICAS=true). */
    @Value("${app.seguridad.docs-publicas:false}")
    private boolean docsPublicas;

    /** La API solo devuelve JSON: nada puede cargar scripts, estilos ni imágenes de ella, ni incrustarla en otra página. */
    private static final String CSP_API = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
    /** Con Swagger abierto la interfaz necesita sus propios scripts y estilos. */
    private static final String CSP_CON_DOCS = "default-src 'self'; script-src 'self' 'unsafe-inline'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'";

    public SecurityConfig(JwtFilter jwtFilter,
                          RateLimitingFilter rateLimitingFilter,
                          @Autowired(required = false) CorrelationIdFilter correlationIdFilter,
                          @Autowired(required = false) LimiteTamanoFilter limiteTamanoFilter,
                          @Autowired(required = false) MetricasTokenFilter metricasTokenFilter) {
        this.jwtFilter = jwtFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.correlationIdFilter = correlationIdFilter != null ? correlationIdFilter : new CorrelationIdFilter();
        this.limiteTamanoFilter = limiteTamanoFilter != null ? limiteTamanoFilter : new LimiteTamanoFilter();
        this.metricasTokenFilter = metricasTokenFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        // HSTS solo se envía por HTTPS (detrás de un proxy hace falta server.forward-headers-strategy)
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(docsPublicas ? CSP_CON_DOCS : CSP_API))
                        .xssProtection(xss -> {})
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"No autorizado\", \"mensaje\": \"Token JWT ausente o inválido\"}");
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes-contacto/publico").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()
                        // Lecturas públicas del sitio. Los endpoints solo-admin dentro de estas rutas
                        // (p. ej. GET /api/lotes) se protegen con @PreAuthorize en el controlador.
                        .requestMatchers(HttpMethod.GET,
                                "/api/proyectos/**", "/api/lotes/**", "/api/ubicaciones/**",
                                "/api/casas-modelo/**", "/api/zonas-comunes/**", "/api/puntos-360/**").permitAll()
                        // Solo las consultas por entidad (/{tipoEntidad}/{id}); el listado completo y el
                        // detalle por ID siguen requiriendo sesión. Para el público el controlador fuerza
                        // que solo se devuelva lo publicado.
                        .requestMatchers(HttpMethod.GET, "/api/multimedia/*/*").permitAll()
                        // Textos institucionales ("Respaldo / Nosotros") de una empresa, solo publicados.
                        .requestMatchers(HttpMethod.GET, "/api/contenidos-institucionales/empresa/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .access((autenticacion, contexto) -> new AuthorizationDecision(
                                        docsPublicas || tieneRol(autenticacion.get(), "ROLE_ADMINISTRADOR")))
                        // Las sondas de salud (Render, Kubernetes) son públicas; las métricas solo con el token de Prometheus
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("METRICAS")
                        .requestMatchers("/actuator/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        // Cualquier persona con sesión puede editar SU perfil (PUT /api/usuarios/{id}); el servicio solo le deja
                        // cambiar nombre y contraseña, y @PreAuthorize comprueba que sea el dueño. Va antes de la regla general.
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*").authenticated()
                        // Regla por defecto: escribir en la API es cosa de administradores. Las excepciones públicas (/api/auth/**,
                        // POST contacto público, /api/chat/**) y el perfil propio ya quedaron arriba. Así un endpoint nuevo
                        // sin @PreAuthorize no queda abierto por olvido.
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated()
                );

        if (metricasTokenFilter != null) {
            http.addFilterBefore(metricasTokenFilter, UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterBefore(limiteTamanoFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    private static boolean tieneRol(Authentication autenticacion, String rol) {
        return autenticacion != null && autenticacion.isAuthenticated()
                && autenticacion.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> originsList = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        configuration.setAllowedOrigins(originsList);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-ID", "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}