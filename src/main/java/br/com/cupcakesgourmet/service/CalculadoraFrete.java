package br.com.cupcakesgourmet.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** RN02 — frete grátis a partir de R$ 100,00; abaixo disso, frete fixo de R$ 10,00. */
@Component
public class CalculadoraFrete {

    public static final BigDecimal LIMITE_FRETE_GRATIS = new BigDecimal("100.00");
    public static final BigDecimal FRETE_PADRAO = new BigDecimal("10.00");

    public BigDecimal calcularFrete(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException("Subtotal inválido: " + subtotal);
        }
        return subtotal.compareTo(LIMITE_FRETE_GRATIS) >= 0 ? new BigDecimal("0.00") : FRETE_PADRAO;
    }
}
