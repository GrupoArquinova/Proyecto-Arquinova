package com.constructora_backend.repository;

import com.constructora_backend.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findByActivoTrue();
    Optional<Empresa> findByNit(String nit);
    boolean existsByNit(String nit);
}
