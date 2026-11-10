package com.constructora_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.constructora_backend.service.LimitadorPasesWallet.Resultado.DIRECCION_RECIENTE;
import static com.constructora_backend.service.LimitadorPasesWallet.Resultado.PERMITIDO;
import static com.constructora_backend.service.LimitadorPasesWallet.Resultado.TOPE_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;

class LimitadorPasesWalletTest {

    private final Instant t0 = Instant.parse("2026-11-09T10:00:00Z");

    @Test
    @DisplayName("Una vez por hora y por dirección, sin importar mayúsculas ni espacios; pasada la hora vuelve a poder")
    void unaPorHora() {
        LimitadorPasesWallet limitador = new LimitadorPasesWallet(new LimitadorVentana(), 100, 100);

        assertThat(limitador.permitir("Ana@correo.com", t0)).isEqualTo(PERMITIDO);
        assertThat(limitador.permitir(" ana@CORREO.com ", t0.plusSeconds(30))).isEqualTo(DIRECCION_RECIENTE);
        assertThat(limitador.permitir("beto@correo.com", t0.plusSeconds(30))).isEqualTo(PERMITIDO);
        assertThat(limitador.permitir("ana@correo.com", t0.plus(Duration.ofMinutes(61)))).isEqualTo(PERMITIDO);
    }

    @Test
    @DisplayName("Tope global por hora: repartir el abuso entre muchas direcciones tampoco pasa, y la dirección rechazada no queda marcada")
    void topeGlobal() {
        LimitadorPasesWallet limitador = new LimitadorPasesWallet(new LimitadorVentana(), 2, 100);

        assertThat(limitador.permitir("a@correo.com", t0)).isEqualTo(PERMITIDO);
        assertThat(limitador.permitir("b@correo.com", t0)).isEqualTo(PERMITIDO);
        assertThat(limitador.permitir("c@correo.com", t0)).isEqualTo(TOPE_GLOBAL);
        assertThat(limitador.permitir("c@correo.com", t0.plus(Duration.ofMinutes(61)))).isEqualTo(PERMITIDO);
    }

    @Test
    @DisplayName("Tope global por día")
    void topeDiario() {
        LimitadorPasesWallet limitador = new LimitadorPasesWallet(new LimitadorVentana(), 100, 1);

        assertThat(limitador.permitir("a@correo.com", t0)).isEqualTo(PERMITIDO);
        assertThat(limitador.permitir("b@correo.com", t0.plus(Duration.ofHours(2)))).isEqualTo(TOPE_GLOBAL);
    }

    @Test
    @DisplayName("Liberar deja a la dirección intentar de nuevo; un correo vacío nunca pasa")
    void liberar() {
        LimitadorPasesWallet limitador = new LimitadorPasesWallet(new LimitadorVentana(), 100, 100);

        assertThat(limitador.permitir("a@correo.com", t0)).isEqualTo(PERMITIDO);
        limitador.liberar("A@correo.com");
        assertThat(limitador.permitir("a@correo.com", t0.plusSeconds(5))).isEqualTo(PERMITIDO);
        assertThat(limitador.permitir("  ", t0)).isEqualTo(DIRECCION_RECIENTE);
        assertThat(limitador.permitir(null, t0)).isEqualTo(DIRECCION_RECIENTE);
    }
}
