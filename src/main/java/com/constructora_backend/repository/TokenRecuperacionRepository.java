package com.constructora_backend.repository;

import com.constructora_backend.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Marca como usados todos los tokens pendientes de un usuario (al emitir uno nuevo o al restablecer la contraseña). */
    @Modifying
    @Query("update TokenRecuperacion t set t.usadoEn = :ahora where t.usuario.id = :usuarioId and t.usadoEn is null")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
