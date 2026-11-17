package br.com.cupcakesgourmet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RN02 — Cálculo de frete")
class CalculadoraFreteTest {

    private final CalculadoraFrete calculadora = new CalculadoraFrete();

    @ParameterizedTest(name = "subtotal {0} -> frete {1}")
    @CsvSource({
            "0.00, 10.00",
            "11.90, 10.00",
            "99.99, 10.00",
            "100.00, 0.00",
            "100.01, 0.00",
            "350.00, 0.00"
    })
    void calculaFretePorFaixa(String subtotal, String freteEsperado) {
        assertThat(calculadora.calcularFrete(new BigDecimal(subtotal))).isEqualByComparingTo(freteEsperado);
    }

    @Test
    void rejeitaSubtotalNegativoOuNulo() {
        assertThatThrownBy(() -> calculadora.calcularFrete(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculadora.calcularFrete(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
