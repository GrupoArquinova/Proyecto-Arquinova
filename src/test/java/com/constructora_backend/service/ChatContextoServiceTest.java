package com.constructora_backend.service;

import com.constructora_backend.entity.*;
import com.constructora_backend.enums.EstadoProyecto;
import com.constructora_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.constructora_backend.enums.TipoProyecto;
import com.constructora_backend.enums.TipoRegistroProyecto;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Datos reales y públicos que se le dan al asistente: proyectos publicados, villas, amenidades, ubicación y lotes por estado. */
@ExtendWith(MockitoExtension.class)
class ChatContextoServiceTest {

    @Mock private ProyectoRepository proyectoRepository;
    @Mock private UbicacionRepository ubicacionRepository;
    @Mock private CasaModeloRepository casaModeloRepository;
    @Mock private ZonaComunRepository zonaComunRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private EtapaRepository etapaRepository;
    @InjectMocks private ChatContextoService servicio;

    private Proyecto elEncanto;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "whatsappEmpresa", "+57 316 865 3715");
        ReflectionTestUtils.setField(servicio, "sitioUrl", "https://arquinova.vercel.app");

        elEncanto = new Proyecto();
        elEncanto.setId(6L);
        elEncanto.setNombre("El Encanto");
        elEncanto.setDescripcion("Proyecto   inspirado\nen el Quindío.");
        elEncanto.setEstadoProyecto(EstadoProyecto.EN_DISENO);
        elEncanto.setDestacado(true);
    }

    private Lote lote(String estado) {
        Etapa etapa = new Etapa();
        etapa.setProyecto(elEncanto);
        EstadoLote e = new EstadoLote();
        e.setNombre(estado);
        Lote l = new Lote();
        l.setEtapa(etapa);
        l.setEstado(e);
        return l;
    }

    @Test
    void incluyeLosDatosRealesDelProyectoSusVillasAmenidadesYLotes() {
        CasaModelo villa = new CasaModelo();
        villa.setNombre("Villa Samán");
        villa.setAreaConstruidaM2(new BigDecimal("293.00"));
        villa.setNumeroHabitaciones((byte) 3);
        villa.setNumeroBanos((byte) 4);
        ZonaComun jacuzzi = new ZonaComun();
        jacuzzi.setNombre("Jacuzzi");
        Ubicacion u = new Ubicacion();
        u.setCiudad("La Tebaida");
        u.setDepartamento("Quindío");

        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.of(u));
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of(villa));
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of(jacuzzi));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(lote("Disponible"), lote("Disponible"), lote("Vendido")));

        String texto = servicio.construir();

        assertTrue(texto.contains("Grupo Arquinova S.A.S."));
        assertTrue(texto.contains("WhatsApp del equipo: +57 316 865 3715"));
        assertTrue(texto.contains("== Proyecto: El Encanto (proyecto destacado) =="));
        assertTrue(texto.contains("Etapa del proyecto: en diseño"));
        assertTrue(texto.contains("Descripción: Proyecto inspirado en el Quindío."));
        assertTrue(texto.contains("Ubicación: La Tebaida, Quindío"));
        assertTrue(texto.contains("Tipología/villa Villa Samán: 293 m² construidos, 3 habitaciones, 4 baños"));
        assertTrue(texto.contains("Amenidades: Jacuzzi"));
        assertTrue(texto.contains("Lotes por estado: 2 disponible, 1 vendido"));
    }

    @Test
    void sinProyectosPublicadosLoDiceEnVezDeInventar() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        assertTrue(servicio.construir().contains("no hay proyectos publicados"));
    }

    @Test
    void elTextoSeGuardaDiezMinutosYDespuesSeVuelveAConsultar() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        Instant t = Instant.parse("2026-10-15T10:00:00Z");

        servicio.obtener(t);
        servicio.obtener(t.plus(Duration.ofMinutes(9)));
        verify(proyectoRepository, times(1)).findByPublicadoTrueAndActivoTrue();

        servicio.obtener(t.plus(Duration.ofMinutes(11)));
        verify(proyectoRepository, times(2)).findByPublicadoTrueAndActivoTrue();
    }

    @Test
    void lasDescripcionesMuyLargasSeRecortan() {
        elEncanto.setDescripcion("x".repeat(2000));
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.empty());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        String texto = servicio.construir();

        assertFalse(texto.contains("x".repeat(500)));
        assertTrue(texto.contains("…"));
    }

    @Test
    void elContextoNuncaTrabajaPrecios() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.empty());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        assertFalse(servicio.construir().toLowerCase().contains("precio"));
    }

    // ---- Empresa, tipo de registro, enlace, fases, lotes y villas ----

    private void sinRelaciones() {
        when(ubicacionRepository.findByProyectoId(anyLong())).thenReturn(Optional.empty());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(anyLong())).thenReturn(List.of());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(anyLong())).thenReturn(List.of());
    }

    private Lote loteDetallado(String codigo, String nombre, String area, String estado) {
        Lote l = lote(estado);
        l.setCodigo(codigo);
        l.setNombre(nombre);
        l.setAreaM2(new BigDecimal(area));
        return l;
    }

    @Test
    void incluyeLosDatosDeLaEmpresaPeroNoLaTrayectoriaNiMisionVisionValores() {
        Empresa e = new Empresa();
        e.setNombre("Grupo Arquinova S.A.S.");
        e.setDescripcion("Coordinamos proyectos de arquitectura e ingeniería.");
        e.setServicios("Diseño arquitectónico, gestión de licencias, construcción.");
        e.setDireccion("Calle 1 # 2-3, Armenia");
        e.setCorreoComercial("comercial@arquinova.test");
        e.setTelefono("606 000 0000");
        e.setWhatsapp("+57 300 000 0000");
        e.setSitioWeb("https://arquinova.test");
        e.setTrayectoria("Más de 20 años de experiencia");
        when(empresaRepository.findByActivoTrue()).thenReturn(List.of(e));
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());

        String texto = servicio.construir();

        assertTrue(texto.contains("Servicios que ofrece: Diseño arquitectónico, gestión de licencias, construcción."));
        assertTrue(texto.contains("Oficina de atención: Calle 1 # 2-3, Armenia"));
        assertTrue(texto.contains("Correo comercial: comercial@arquinova.test"));
        assertTrue(texto.contains("Teléfono: 606 000 0000"));
        assertTrue(texto.contains("WhatsApp del equipo: +57 300 000 0000"));
        assertTrue(texto.contains("Sitio web: https://arquinova.test"));
        assertFalse(texto.contains("20 años"), "la trayectoria no se le da al asistente");
        String bajo = texto.toLowerCase();
        assertFalse(bajo.contains("misión") || bajo.contains("visión") || bajo.contains("valores"));
    }

    @Test
    void distingueOfertaComercialDePortafolioYDaElEnlaceYLaParticipacion() {
        Proyecto portafolio = new Proyecto();
        portafolio.setId(9L);
        portafolio.setNombre("Casa Mirador");
        portafolio.setTipoRegistro(TipoRegistroProyecto.PORTAFOLIO);
        portafolio.setParticipacion("Estudios y diseño arquitectónico");
        portafolio.setTipoProyecto(TipoProyecto.RESIDENCIAL);
        portafolio.setDestacado(false);
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto, portafolio));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        sinRelaciones();

        String texto = servicio.construir();

        assertTrue(texto.contains("Tipo de registro: Oferta comercial: tiene unidades a la venta"));
        assertTrue(texto.contains("Tipo de registro: Caso de portafolio: Arquinova participó con estudios, diseño o gestión; no es una oferta de venta"));
        assertTrue(texto.contains("Participación de Arquinova: Estudios y diseño arquitectónico"));
        assertTrue(texto.contains("Tipo de proyecto: residencial"));
        assertTrue(texto.contains("Enlace: https://arquinova.vercel.app/proyectos/6"));
        assertTrue(texto.contains("Enlace: https://arquinova.vercel.app/proyectos/9"));
    }

    @Test
    void sinSitioConfiguradoNoHayEnlaces() {
        ReflectionTestUtils.setField(servicio, "sitioUrl", "");
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        sinRelaciones();

        assertFalse(servicio.construir().contains("/proyectos/"));
    }

    @Test
    void incluyeLasFasesActivasEnOrdenYLaDescripcionCortaDeLasAmenidades() {
        Etapa f1 = new Etapa();
        f1.setNombre("Fase 1");
        f1.setDescripcion("Primeras villas");
        Etapa f2 = new Etapa();
        f2.setNombre("Fase 2");
        ZonaComun piscina = new ZonaComun();
        piscina.setNombre("Piscina");
        piscina.setDescripcion("Piscina   para adultos y niños");
        ZonaComun bbq = new ZonaComun();
        bbq.setNombre("Zona BBQ");
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.empty());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(etapaRepository.findByProyectoIdAndActivoTrueOrderByOrdenAsc(6L)).thenReturn(List.of(f1, f2));
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of(piscina, bbq));

        String texto = servicio.construir();

        assertTrue(texto.contains("Fases del proyecto: Fase 1 (Primeras villas); Fase 2\n"));
        assertTrue(texto.contains("Amenidades: Piscina (Piscina para adultos y niños); Zona BBQ\n"));
    }

    @Test
    void laVillaDiceSiTieneTourVirtualYPlanosSinMostrarLasDirecciones() {
        CasaModelo conTodo = new CasaModelo();
        conTodo.setNombre("Villa Samán");
        conTodo.setTourVirtualUrl("https://tour.test/secreto-123");
        conTodo.setPlanoUrl("https://planos.test/plano-456.pdf");
        CasaModelo sinNada = new CasaModelo();
        sinNada.setNombre("Villa Guadua");
        sinNada.setTourVirtualUrl("  ");
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(ubicacionRepository.findByProyectoId(6L)).thenReturn(Optional.empty());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of());
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(6L)).thenReturn(List.of(conTodo, sinNada));

        String texto = servicio.construir();

        assertTrue(texto.contains("Tipología/villa Villa Samán: tiene tour virtual, tiene planos"));
        assertTrue(texto.contains("Tipología/villa Villa Guadua: sin tour virtual, sin planos"));
        assertFalse(texto.contains("tour.test"));
        assertFalse(texto.contains("planos.test"));
    }

    @Test
    void detallaLosLotesDisponiblesConCodigoNombreYAreaOrdenadosPorCodigo() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        sinRelaciones();
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(
                loteDetallado("L-02", null, "500.50", "Disponible"),
                loteDetallado("L-01", "Mirador", "420.00", "Disponible"),
                loteDetallado("L-03", null, "800", "Vendido")));

        String texto = servicio.construir();

        assertTrue(texto.contains("Lotes por estado: 2 disponible, 1 vendido"));
        assertTrue(texto.contains("Áreas de los lotes: de 420 m² a 800 m²"));
        assertTrue(texto.contains("Lotes disponibles: L-01 (Mirador) 420 m²; L-02 500.5 m²\n"));
        assertFalse(texto.contains("L-03 "), "un lote vendido no se detalla como disponible");
    }

    @Test
    void losLotesDisponiblesSeLimitanA15YSeIndicaCuantosMasHay() {
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of(elEncanto));
        sinRelaciones();
        List<Lote> lotes = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) lotes.add(loteDetallado(String.format("L-%02d", i), null, "300", "Disponible"));
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(lotes);

        String texto = servicio.construir();

        assertTrue(texto.contains("L-15 300 m²"));
        assertFalse(texto.contains("L-16 300 m²"));
        assertTrue(texto.contains("; y 5 más"));
    }

    @Test
    void elTextoTotalNoSuperaElTopeYAvisaDeLosProyectosOmitidos() {
        List<Proyecto> muchos = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Proyecto p = new Proyecto();
            p.setId((long) i);
            p.setNombre(String.format("Proyecto %02d", i));
            p.setDescripcion("d".repeat(450) + " fin");
            p.setDestacado(false);
            muchos.add(p);
        }
        when(proyectoRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(muchos);
        when(loteRepository.findByPublicadoTrueAndActivoTrue()).thenReturn(List.of());
        when(ubicacionRepository.findByProyectoId(anyLong())).thenReturn(Optional.empty());
        when(zonaComunRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(anyLong())).thenReturn(List.of());
        // Villas largas para engordar cada proyecto
        CasaModelo v = new CasaModelo();
        v.setNombre("Villa");
        v.setDescripcion("v".repeat(450));
        when(casaModeloRepository.findByProyectoIdAndPublicadoTrueAndActivoTrue(anyLong())).thenReturn(List.of(v, v, v));

        String texto = servicio.construir();

        assertTrue(texto.length() <= ChatContextoService.MAX_CONTEXTO + 200, "largo: " + texto.length());
        assertTrue(texto.contains("proyecto(s) más"));
    }
}
