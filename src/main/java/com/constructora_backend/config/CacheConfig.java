package com.constructora_backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración central de caché empresarial distribuida para el backend.
 * Soporta Redis con serialización JSON, TTLs personalizados por catálogo y tolerancia a fallos.
 * Incluye fallback automático a memoria local cuando Redis no está presente (ej. testing o entornos aislados).
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    public static final String CACHE_PROYECTOS = "proyectos_publicos";
    public static final String CACHE_UBICACIONES = "ubicaciones";
    public static final String CACHE_CASAS_MODELO = "casas_modelo";
    public static final String CACHE_ZONAS_COMUNES = "zonas_comunes";

    @Bean
    @Primary
    public CacheManager cacheManager(@Autowired(required = false) RedisConnectionFactory connectionFactory) {
        if (connectionFactory != null) {
            try {
                log.info("Inicializando RedisCacheManager distribuido con TTL y serialización JSON...");
                return buildRedisCacheManager(connectionFactory);
            } catch (Exception ex) {
                log.warn("No fue posible inicializar RedisCacheManager, recurriendo a ConcurrentMapCacheManager local: {}", ex.getMessage());
            }
        }

        log.info("Inicializando ConcurrentMapCacheManager en memoria local...");
        ConcurrentMapCacheManager localCacheManager = new ConcurrentMapCacheManager();
        localCacheManager.setCacheNames(List.of(
                CACHE_PROYECTOS,
                CACHE_UBICACIONES,
                CACHE_CASAS_MODELO,
                CACHE_ZONAS_COMUNES
        ));
        return localCacheManager;
    }

    private RedisCacheManager buildRedisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_PROYECTOS, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put(CACHE_UBICACIONES, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put(CACHE_CASAS_MODELO, defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put(CACHE_ZONAS_COMUNES, defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Fallo de lectura en caché '{}' para clave '{}'. Continuando sin caché: {}", 
                        cache != null ? cache.getName() : "desconocida", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Fallo de escritura en caché '{}' para clave '{}': {}", 
                        cache != null ? cache.getName() : "desconocida", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Fallo de desalojo en caché '{}' para clave '{}': {}", 
                        cache != null ? cache.getName() : "desconocida", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Fallo de limpieza en caché '{}': {}", 
                        cache != null ? cache.getName() : "desconocida", exception.getMessage());
            }
        };
    }
}
