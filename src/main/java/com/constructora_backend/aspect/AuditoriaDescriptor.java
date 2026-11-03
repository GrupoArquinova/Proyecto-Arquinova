package com.constructora_backend.aspect;

import com.constructora_backend.entity.CasaModelo;
import com.constructora_backend.entity.ContenidoInstitucional;
import com.constructora_backend.entity.Empresa;
import com.constructora_backend.entity.EstadoLote;
import com.constructora_backend.entity.EstadoSolicitud;
import com.constructora_backend.entity.Etapa;
import com.constructora_backend.entity.Lote;
import com.constructora_backend.entity.Multimedia;
import com.constructora_backend.entity.Proyecto;
import com.constructora_backend.entity.Punto360;
import com.constructora_backend.entity.SolicitudContacto;
import com.constructora_backend.entity.Ubicacion;
import com.constructora_backend.entity.Usuario;
import com.constructora_backend.entity.ZonaComun;
import com.constructora_backend.entity.ZonaComunImagen;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Convierte un evento auditable en una frase legible ("Editó proyecto 'El Encanto' (cambió: descripcion, publicado: no → sí)").
 * <p>
 * Antes de ejecutar la operación toma una instantánea de la fila afectada (nombre y valores) y otra después, y compara.
 * Qué queda en el historial:
 * <ul>
 *   <li>Los NOMBRES de los campos que cambiaron; el valor solo cuando es un sí/no, un estado de lista (enum) o una relación
 *       (estado, rol, etapa...), que son datos de gestión y no personales.</li>
 *   <li>Nunca contraseñas, hashes, tokens ni fechas internas: esos campos se descartan por nombre (ver {@link #esCampoSensible}).</li>
 *   <li>Nunca el texto de los campos libres (descripciones, mensajes, observaciones).</li>
 * </ul>
 * Si algo falla (entidad desconocida, error de lectura), el aspecto usa la descripción fija de @Auditable: auditar nunca
 * rompe la operación del usuario.
 */
@Component
public class AuditoriaDescriptor {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaDescriptor.class);

    /** Largo de la columna descripcion de la tabla auditoria. */
    static final int MAX_DESCRIPCION = 500;

    /** Cómo se llama cada entidad auditada y qué atributo la identifica de forma legible (el primero con valor). */
    private record Registro(Class<?> clase, String etiqueta, List<String> atributosNombre) {
    }

    private static final Map<String, Registro> REGISTRO = Map.ofEntries(
            Map.entry("PROYECTOS", new Registro(Proyecto.class, "proyecto", List.of("nombre"))),
            Map.entry("CASAS_MODELO", new Registro(CasaModelo.class, "casa modelo", List.of("nombre"))),
            Map.entry("EMPRESAS", new Registro(Empresa.class, "empresa", List.of("nombre"))),
            Map.entry("ESTADOS_LOTE", new Registro(EstadoLote.class, "estado de lote", List.of("nombre"))),
            Map.entry("ESTADOS_SOLICITUD", new Registro(EstadoSolicitud.class, "estado de solicitud", List.of("nombre"))),
            Map.entry("ETAPAS", new Registro(Etapa.class, "etapa", List.of("nombre"))),
            Map.entry("LOTES", new Registro(Lote.class, "lote", List.of("codigo", "nombre"))),
            Map.entry("MULTIMEDIA", new Registro(Multimedia.class, "recurso multimedia", List.of("titulo", "nombreArchivo"))),
            Map.entry("UBICACIONES", new Registro(Ubicacion.class, "ubicación", List.of("ciudad", "direccion"))),
            Map.entry("USUARIOS", new Registro(Usuario.class, "usuario", List.of("nombreCompleto"))),
            Map.entry("ZONAS_COMUNES", new Registro(ZonaComun.class, "zona común", List.of("nombre"))),
            Map.entry("ZONA_COMUN_IMAGENES", new Registro(ZonaComunImagen.class, "imagen de zona común", List.of("titulo"))),
            Map.entry("PUNTOS_360", new Registro(Punto360.class, "punto 360°", List.of("etiqueta"))),
            Map.entry("CONTENIDOS_INSTITUCIONALES", new Registro(ContenidoInstitucional.class, "contenido institucional", List.of("titulo", "seccion"))),
            // Una solicitud no se identifica por el nombre de la persona (dato personal): solo por su número
            Map.entry("SOLICITUDES_CONTACTO", new Registro(SolicitudContacto.class, "solicitud de contacto", List.of())));

    /** Acciones con una frase propia (el resto se arma con el verbo según el prefijo de la acción). */
    private static final Map<String, String> FRASES_PROPIAS = Map.of(
            "CAMBIAR_ESTADO_LOTE", "Cambió el estado del lote",
            "ACTIVAR_DESACTIVAR_LOTE", "Activó o desactivó el lote",
            "ATENDER_SOLICITUD_CONTACTO", "Atendió la solicitud de contacto");

    private static final List<String[]> VERBOS_POR_PREFIJO = List.of(
            new String[]{"CREAR_", "Creó"},
            new String[]{"ACTUALIZAR_", "Editó"},
            new String[]{"ELIMINAR_", "Eliminó"},
            new String[]{"DESACTIVAR_", "Desactivó"},
            new String[]{"MARCAR_PRINCIPAL_", "Marcó como principal"});

    /** Campos que nunca entran al historial, ni por nombre ni por valor. */
    private static final Set<String> CAMPOS_IGNORADOS = Set.of(
            "creadoEn", "actualizadoEn", "atendidaEn", "ultimoAccesoEn", "ip", "userAgent",
            "creadoPor", "actualizadoPor", "atendidaPor", "usuario");

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate transaccion;

    public AuditoriaDescriptor(PlatformTransactionManager transactionManager) {
        this.transaccion = new TransactionTemplate(transactionManager);
        this.transaccion.setReadOnly(true);
    }

    /** Lo que se sabe de una fila en un instante: su nombre legible y los valores de sus campos. */
    public record Instantanea(String nombre, Map<String, String> valores) {
    }

    /** true si la entidad auditada tiene descripción detallada. */
    public boolean conoce(String entidad) {
        return entidad != null && REGISTRO.containsKey(entidad);
    }

    /**
     * Toma la instantánea de una fila, o null si no existe, no se conoce la entidad o la lectura falla.
     * Corre en una transacción de solo lectura propia: la petición no mantiene la sesión abierta (open-in-view apagado).
     */
    public Instantanea capturar(String entidad, Long id) {
        Registro registro = entidad == null ? null : REGISTRO.get(entidad);
        if (registro == null || id == null) {
            return null;
        }
        try {
            return transaccion.execute(estado -> {
                EntityType<?> tipo = em.getMetamodel().entity(registro.clase());
                Object clave = convertirId(id, tipo.getIdType().getJavaType());
                Object encontrado = em.find(registro.clase(), clave);
                if (encontrado == null) {
                    return null;
                }
                Object entidadReal = Hibernate.unproxy(encontrado);
                return new Instantanea(nombreDe(entidadReal, registro.atributosNombre()), valoresDe(entidadReal, tipo));
            });
        } catch (Exception ex) {
            log.debug("Auditoría: no se pudo leer {} #{}: {}", entidad, id, ex.getMessage());
            return null;
        }
    }

    /**
     * Arma la descripción legible, o devuelve {@code porDefecto} si la acción/entidad no tiene frase propia.
     *
     * @param antes   instantánea previa a la operación (null en altas)
     * @param despues instantánea posterior (null en bajas)
     */
    public String describir(String accion, String entidad, Long id, Instantanea antes, Instantanea despues, String porDefecto) {
        Registro registro = entidad == null ? null : REGISTRO.get(entidad);
        String verbo = verboDe(accion);
        if (registro == null || verbo == null) {
            return porDefecto;
        }
        String nombre = despues != null && despues.nombre() != null ? despues.nombre() : (antes != null ? antes.nombre() : null);
        boolean frasePropia = FRASES_PROPIAS.containsKey(accion);
        String cambios = (antes != null && despues != null) ? resumirCambios(antes.valores(), despues.valores()) : "";
        return construir(verbo, frasePropia ? null : registro.etiqueta(), nombre, id, cambios);
    }

    // ------------------------------------------------------------------ piezas puras (con pruebas propias)

    /** "Editó proyecto 'El Encanto' (cambió: publicado: no → sí)"; sin nombre usa "#id". Recorta al largo de la columna. */
    static String construir(String verbo, String etiqueta, String nombre, Long id, String cambios) {
        StringBuilder sb = new StringBuilder(verbo);
        if (etiqueta != null && !etiqueta.isBlank()) {
            sb.append(' ').append(etiqueta);
        }
        if (nombre != null && !nombre.isBlank()) {
            sb.append(" '").append(nombre.replaceAll("[\\r\\n\\p{Cntrl}]", " ").trim()).append('\'');
        } else if (id != null) {
            sb.append(" #").append(id);
        }
        if (cambios != null && !cambios.isBlank()) {
            sb.append(" (cambió: ").append(cambios).append(')');
        }
        String texto = sb.toString();
        return texto.length() > MAX_DESCRIPCION ? texto.substring(0, MAX_DESCRIPCION - 3) + "..." : texto;
    }

    /**
     * Compara dos instantáneas. Los valores guardados llevan el prefijo "V:" cuando se pueden mostrar (sí/no, enums, relaciones)
     * y "T:" cuando solo se debe nombrar el campo (texto libre, números, URLs).
     */
    static String resumirCambios(Map<String, String> antes, Map<String, String> despues) {
        // En orden alfabético: el orden de los atributos que da JPA no está garantizado y el texto debe ser estable
        List<String> cambios = new ArrayList<>();
        for (Map.Entry<String, String> e : new java.util.TreeMap<>(despues).entrySet()) {
            String anterior = antes.get(e.getKey());
            String actual = e.getValue();
            if (Objects.equals(anterior, actual)) {
                continue;
            }
            if (actual != null && actual.startsWith("V:")) {
                cambios.add(e.getKey() + ": " + mostrar(anterior) + " → " + mostrar(actual));
            } else {
                cambios.add(e.getKey());
            }
        }
        return String.join(", ", cambios);
    }

    private static String mostrar(String valor) {
        if (valor == null) {
            return "—";
        }
        return valor.startsWith("V:") ? valor.substring(2) : valor;
    }

    /** Nombres de campos que jamás se registran, ni siquiera su nombre. */
    static boolean esCampoSensible(String atributo) {
        String minuscula = atributo.toLowerCase(Locale.ROOT);
        return minuscula.contains("password") || minuscula.contains("hash") || minuscula.contains("token")
                || minuscula.contains("secret") || minuscula.contains("clave");
    }

    private static String verboDe(String accion) {
        if (accion == null) {
            return null;
        }
        String propia = FRASES_PROPIAS.get(accion);
        if (propia != null) {
            return propia;
        }
        for (String[] par : VERBOS_POR_PREFIJO) {
            if (accion.startsWith(par[0])) {
                return par[1];
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ lectura de entidades

    private static Object convertirId(Long id, Class<?> tipoId) {
        if (Integer.class.equals(tipoId) || int.class.equals(tipoId)) {
            return id.intValue();
        }
        if (Short.class.equals(tipoId) || short.class.equals(tipoId)) {
            return id.shortValue();
        }
        return id;
    }

    private static String nombreDe(Object entidad, List<String> atributos) {
        for (String atributo : atributos) {
            Object valor = invocarGetter(entidad, atributo);
            if (valor != null && !valor.toString().isBlank()) {
                return valor.toString();
            }
        }
        return null;
    }

    private Map<String, String> valoresDe(Object entidad, EntityType<?> tipo) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (SingularAttribute<?, ?> atributo : tipo.getSingularAttributes()) {
            String nombre = atributo.getName();
            if (atributo.isId() || CAMPOS_IGNORADOS.contains(nombre) || esCampoSensible(nombre)) {
                continue;
            }
            try {
                Object valor = leer(entidad, atributo);
                if (atributo.isAssociation()) {
                    valores.put(nombre, valor == null ? null : "V:" + etiquetaDeRelacion(valor));
                } else {
                    valores.put(nombre, codificar(valor));
                }
            } catch (Exception ex) {
                // Un campo que no se puede leer (relación perezosa sin sesión) simplemente no se compara
                log.debug("Auditoría: campo {} no legible: {}", nombre, ex.getMessage());
            }
        }
        return valores;
    }

    private static Object leer(Object entidad, Attribute<?, ?> atributo) throws ReflectiveOperationException {
        Member miembro = atributo.getJavaMember();
        if (miembro instanceof Field campo) {
            campo.setAccessible(true);
            return campo.get(entidad);
        }
        if (miembro instanceof Method metodo) {
            metodo.setAccessible(true);
            return metodo.invoke(entidad);
        }
        return invocarGetter(entidad, atributo.getName());
    }

    /** Lo que se muestra de una relación: su nombre legible (nombre, código...) o su ID. */
    private static String etiquetaDeRelacion(Object relacionada) {
        Object real = Hibernate.unproxy(relacionada);
        for (String atributo : List.of("nombre", "codigo", "seccion", "titulo", "nombreCompleto")) {
            Object valor = invocarGetter(real, atributo);
            if (valor != null && !valor.toString().isBlank()) {
                return valor.toString();
            }
        }
        Object id = invocarGetter(real, "id");
        return id == null ? "?" : "#" + id;
    }

    /** "V:" + valor mostrable (sí/no, enum) o "T:" + huella del valor para detectar el cambio sin guardarlo. */
    private static String codificar(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Boolean b) {
            return "V:" + (b ? "sí" : "no");
        }
        if (valor instanceof Enum<?> e) {
            return "V:" + e.name();
        }
        if (valor instanceof BigDecimal d) {
            return "T:" + d.stripTrailingZeros().toPlainString();
        }
        // Huella: sirve para saber que cambió; el texto en sí no se copia al historial
        return "T:" + valor.toString().hashCode() + ":" + valor.toString().length();
    }

    private static Object invocarGetter(Object objeto, String atributo) {
        try {
            String getter = "get" + Character.toUpperCase(atributo.charAt(0)) + atributo.substring(1);
            return objeto.getClass().getMethod(getter).invoke(objeto);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
