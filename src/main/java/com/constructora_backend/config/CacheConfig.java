package com.constructora_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración central de caché para el backend.
 * Reduce la carga en MySQL almacenando en memoria las consultas frecuentes de catálogos públicos.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PROYECTOS = "proyectos_publicos";
    public static final String CACHE_UBICACIONES = "ubicaciones";
    public static final String CACHE_CASAS_MODELO = "casas_modelo";
    public static final String CACHE_ZONAS_COMUNES = "zonas_comunes";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                CACHE_PROYECTOS,
                CACHE_UBICACIONES,
                CACHE_CASAS_MODELO,
                CACHE_ZONAS_COMUNES
        ));
        return cacheManager;
    }
}
