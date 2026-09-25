package com.constructora_backend.config;

import com.constructora_backend.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter,
                          RateLimitingFilter rateLimitingFilter,
                          @Autowired(required = false) CorrelationIdFilter correlationIdFilter,
                          @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository) {
        this.jwtFilter = jwtFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.correlationIdFilter = correlationIdFilter != null ? correlationIdFilter : new CorrelationIdFilter();
        this.clientRegistrationRepository = clientRegistrationRepository;
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
                        .requestMatchers(HttpMethod.GET, "/api/proyectos/**", "/api/lotes/**", "/api/ubicaciones/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes-contacto/publico").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/proyectos/**", "/api/lotes/**", "/api/ubicaciones/**",
                                "/api/casas-modelo/**", "/api/zonas-comunes/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated()
                );

        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(authorization -> authorization
                            .baseUri("/oauth2/authorization")
                    )
            );
        }

        http.addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

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