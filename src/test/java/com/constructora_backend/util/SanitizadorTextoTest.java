package com.constructora_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizadorTextoTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("Quita etiquetas HTML y el contenido de los bloques peligrosos")
    @CsvSource(delimiter = '|', value = {
            "<script>alert(1)</script>Hola|Hola",
            "Hola <b>mundo</b>|Hola mundo",
            "<img src=x onerror=alert(1)>Texto|Texto",
            "<SCRIPT SRC=//evil.test/x.js></SCRIPT>ok|ok",
            "<style>body{display:none}</style>Hola|Hola",
            "<iframe src=\"https://evil.test\"></iframe>Hola|Hola",
            "<a href=\"javascript:alert(1)\">clic</a>|clic",
            "<scr<script>ipt>alert(1)</scr</script>ipt>Hola|Hola",
            "Nombre <script|Nombre script",
            "<svg/onload=alert(1)>Hola|Hola",
            "<!-- comentario -->Hola|Hola"
    })
    void quitaMarcado(String entrada, String esperado) {
        assertThat(SanitizadorTexto.limpiar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @DisplayName("No toca el texto normal: símbolos de menor y mayor, acentos, comillas, ñ, emoji, URL y números")
    @ValueSource(strings = {
            "Hola, ¿cómo estás? Quiero información de la villa 3.",
            "Me gustan las casas <3",
            "Presupuesto a < 200 millones y 3 > 2 habitaciones",
            "Tom & Jerry's \"casa\" 100% ñandú áéíóú üÜ",
            "https://www.ejemplo.com/ruta?a=1&b=2#ancla",
            "+57 316 865 3715",
            "Quiero verla el lunes 😊👍",
            "Línea 1\nLínea 2\r\n\tcon tabulación"
    })
    void noTocaTextoNormal(String texto) {
        assertThat(SanitizadorTexto.limpiar(texto)).isEqualTo(texto);
    }

    @Test
    @DisplayName("Quita caracteres de control y los que invierten el sentido del texto, y conserva saltos de línea")
    void quitaControl() {
        assertThat(SanitizadorTexto.limpiar("a\u0000b\u0007c\u001Bd")).isEqualTo("abcd");
        assertThat(SanitizadorTexto.limpiar("archivo‮gpj.exe")).isEqualTo("archivogpj.exe");
        assertThat(SanitizadorTexto.limpiar("uno\ndos\r\ntres\tcuatro")).isEqualTo("uno\ndos\r\ntres\tcuatro");
    }

    @Test
    @DisplayName("Unifica los acentos (NFC): 'é' escrito con dos caracteres queda como uno")
    void normalizaAcentos() {
        assertThat(SanitizadorTexto.limpiar("Café")).isEqualTo("Café");
    }

    @Test
    @DisplayName("null y vacío se devuelven tal cual; una entrada hostil muy larga termina rápido")
    void bordes() {
        assertThat(SanitizadorTexto.limpiar(null)).isNull();
        assertThat(SanitizadorTexto.limpiar("")).isEmpty();
        String hostil = "<".repeat(5000) + "a>".repeat(5000);
        long inicio = System.nanoTime();
        SanitizadorTexto.limpiar(hostil);
        assertThat((System.nanoTime() - inicio) / 1_000_000).isLessThan(2000);
    }

    @Test
    @DisplayName("Reconoce por el nombre los campos que no se deben tocar")
    void camposSensibles() {
        for (String nombre : new String[]{"password", "nuevaPassword", "passwordActual", "token", "captchaToken", "refreshToken", "contrasena", "clave", "Authorization"}) {
            assertThat(SanitizadorTexto.esCampoSensible(nombre)).as(nombre).isTrue();
        }
        for (String nombre : new String[]{"nombre", "mensaje", "correo", "descripcion", "url", null}) {
            assertThat(SanitizadorTexto.esCampoSensible(nombre)).as(String.valueOf(nombre)).isFalse();
        }
    }

    @Test
    @DisplayName("Detecta lo que Excel podría leer como fórmula")
    void formulas() {
        for (String f : new String[]{"=1+1", "+57 316", "-2+3", "@SUM(A1)", "\tcmd", "\r=1", "=HYPERLINK(\"http://evil\",\"x\")"}) {
            assertThat(SanitizadorTexto.pareceFormula(f)).as(f).isTrue();
        }
        for (String f : new String[]{"Ana", "1+1", "a=b", " =1", "", null}) {
            assertThat(SanitizadorTexto.pareceFormula(f)).as(String.valueOf(f)).isFalse();
        }
    }
}
