package com.constructora_backend.repository;

import com.constructora_backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    /** Cuántos administradores activos hay (el rol puede estar guardado con o sin el prefijo ROLE_). */
    @Query("select count(u) from Usuario u where u.activo = true and (u.rol.nombre = 'ADMINISTRADOR' or u.rol.nombre = 'ROLE_ADMINISTRADOR')")
    long contarAdministradoresActivos();
}
