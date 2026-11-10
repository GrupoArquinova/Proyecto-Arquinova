package com.constructora_backend.documento;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/**
 * Una columna de un reporte. La misma definición arma la tabla del Excel y la del PDF, para que los dos
 * muestren lo mismo.
 *
 * @param ancho ancho aproximado en caracteres; en el PDF se usa como peso relativo entre columnas
 * @param sumar si es true, el reporte agrega una fila de totales con la suma de la columna
 */
public record Columna<T>(String titulo, int ancho, Tipo tipo, Function<T, Object> valor, boolean sumar) {

    public enum Tipo { TEXTO, ENTERO, DECIMAL, PORCENTAJE, FECHA, FECHA_HORA, SI_NO }

    private static final Locale CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static <T> Columna<T> texto(String titulo, int ancho, Function<T, Object> valor) {
        return new Columna<>(titulo, ancho, Tipo.TEXTO, valor, false);
    }

    public static <T> Columna<T> entero(String titulo, int ancho, Function<T, Object> valor, boolean sumar) {
        return new Columna<>(titulo, ancho, Tipo.ENTERO, valor, sumar);
    }

    public static <T> Columna<T> decimal(String titulo, int ancho, Function<T, Object> valor, boolean sumar) {
        return new Columna<>(titulo, ancho, Tipo.DECIMAL, valor, sumar);
    }

    public static <T> Columna<T> porcentaje(String titulo, int ancho, Function<T, Object> valor) {
        return new Columna<>(titulo, ancho, Tipo.PORCENTAJE, valor, false);
    }

    public static <T> Columna<T> fecha(String titulo, int ancho, Function<T, Object> valor) {
        return new Columna<>(titulo, ancho, Tipo.FECHA, valor, false);
    }

    public static <T> Columna<T> fechaHora(String titulo, int ancho, Function<T, Object> valor) {
        return new Columna<>(titulo, ancho, Tipo.FECHA_HORA, valor, false);
    }

    public static <T> Columna<T> siNo(String titulo, int ancho, Function<T, Object> valor) {
        return new Columna<>(titulo, ancho, Tipo.SI_NO, valor, false);
    }

    /** Las cifras se alinean a la derecha. */
    public boolean esNumerica() {
        return tipo == Tipo.ENTERO || tipo == Tipo.DECIMAL || tipo == Tipo.PORCENTAJE;
    }

    /** El valor tal como se imprime en el PDF (el Excel guarda el valor real con su formato). */
    public String formatear(Object v) {
        if (v == null) return "";
        return switch (tipo) {
            case ENTERO -> v instanceof Number n ? NumberFormat.getIntegerInstance(CO).format(n.longValue()) : v.toString();
            case DECIMAL -> v instanceof Number n ? decimales(2).format(n.doubleValue()) : v.toString();
            case PORCENTAJE -> v instanceof Number n ? decimales(2).format(n.doubleValue()) + " %" : v.toString();
            case FECHA -> v instanceof LocalDate d ? d.format(FECHA) : v.toString();
            case FECHA_HORA -> v instanceof LocalDateTime d ? d.format(FECHA_HORA) : v.toString();
            case SI_NO -> Boolean.TRUE.equals(v) ? "Sí" : "No";
            case TEXTO -> v.toString();
        };
    }

    private static NumberFormat decimales(int n) {
        NumberFormat f = NumberFormat.getNumberInstance(CO);
        f.setMinimumFractionDigits(n);
        f.setMaximumFractionDigits(n);
        return f;
    }
}
