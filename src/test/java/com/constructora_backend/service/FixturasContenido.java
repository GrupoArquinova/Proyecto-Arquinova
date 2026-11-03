package com.constructora_backend.service;

import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.enums.TipoProyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos mínimos para las pruebas de integración con la base real. Cada prueba corre en una transacción que se revierte,
 * así que nada de esto queda en la base. Los nombres llevan un sufijo aleatorio para no chocar con datos existentes.
 */
final class FixturasContenido {

    private FixturasContenido() {
    }

    static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    static Empresa empresa(EntityManager em) {
        Empresa e = Empresa.builder()
                .nombre("Grupo Prueba " + sufijo())
                .correoComercial("comercial@prueba.test")
                .whatsapp("+57 300 000 0000")
                .sitioWeb("https://prueba.test")
                .direccion("Calle 1 # 2-3, Armenia")
                .activo(true)
                .build();
        em.persist(e);
        em.flush();
        return e;
    }

    static Proyecto proyecto(EntityManager em, Empresa empresa, String nombre, boolean publicado, boolean activo, TipoRegistroProyecto tipo) {
        Proyecto p = new Proyecto();
        p.setEmpresa(empresa);
        p.setNombre(nombre);
        p.setSlug("prueba-" + sufijo());
        p.setDescripcion("Diseño sostenible en el Quindío, con vistas al paisaje cafetero.");
        p.setEstadoProyecto(EstadoProyecto.EN_CONSTRUCCION);
        p.setTipoProyecto(TipoProyecto.RESIDENCIAL);
        p.setTipoRegistro(tipo);
        p.setPublicado(publicado);
        p.setActivo(activo);
        em.persist(p);
        em.flush();
        return p;
    }

    static Ubicacion ubicacion(EntityManager em, Proyecto p) {
        Ubicacion u = new Ubicacion();
        u.setProyecto(p);
        u.setCiudad("Armenia");
        u.setDepartamento("Quindío");
        u.setDireccion("Vereda La Esperanza");
        em.persist(u);
        em.flush();
        return u;
    }

    static CasaModelo villa(EntityManager em, Proyecto p, String nombre) {
        CasaModelo c = new CasaModelo();
        c.setProyecto(p);
        c.setNombre(nombre);
        c.setAreaConstruidaM2(new BigDecimal("180.50"));
        c.setNumeroHabitaciones((byte) 3);
        c.setNumeroBanos((byte) 4);
        c.setPublicado(true);
        c.setActivo(true);
        em.persist(c);
        em.flush();
        return c;
    }

    static ZonaComun zona(EntityManager em, Proyecto p, String nombre) {
        ZonaComun z = new ZonaComun();
        z.setProyecto(p);
        z.setNombre(nombre);
        z.setPublicado(true);
        z.setActivo(true);
        em.persist(z);
        em.flush();
        return z;
    }

    static Etapa etapa(EntityManager em, Proyecto p, String nombre) {
        Etapa e = new Etapa();
        e.setProyecto(p);
        e.setNombre(nombre);
        e.setActivo(true);
        em.persist(e);
        em.flush();
        return e;
    }

    /** El estado de lote con ese nombre; si el catálogo local no lo tiene, se crea (dentro de la transacción de la prueba). */
    static EstadoLote estadoLote(EntityManager em, String nombre) {
        var existentes = em.createQuery("select e from EstadoLote e where upper(e.nombre) = :n", EstadoLote.class)
                .setParameter("n", nombre.toUpperCase()).getResultList();
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        EstadoLote e = new EstadoLote();
        e.setNombre(nombre);
        e.setOrden(9);
        e.setActivo(true);
        em.persist(e);
        em.flush();
        return e;
    }

    static Lote lote(EntityManager em, Etapa etapa, EstadoLote estado, String codigo, String area) {
        Lote l = new Lote();
        l.setEtapa(etapa);
        l.setEstado(estado);
        l.setCodigo(codigo);
        l.setAreaM2(new BigDecimal(area));
        l.setPublicado(true);
        l.setActivo(true);
        em.persist(l);
        em.flush();
        return l;
    }
}
