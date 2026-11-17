package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN09 — Geração do PIX (BR Code)")
class PixServiceTest {

    private final PixService pix = new PixService(new AppProperties(null, null,
            new AppProperties.Pix("pix@cupcakesgourmet.com.br", "Cupcakes Gourmet Confeitaria Artesanal", "São Paulo")));

    @Test
    void crc16CcittFalseConfereComValorDeReferencia() {
        // Valor de verificação padrão do CRC-16/CCITT-FALSE para "123456789"
        assertThat(PixService.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    void geraBrCodeComCamposObrigatoriosEValor() {
        String codigo = pix.gerarCodigo(42L, new BigDecimal("80.5"));

        assertThat(codigo).startsWith("000201");
        assertThat(codigo).contains("0014br.gov.bcb.pix");
        assertThat(codigo).contains("5303986");           // moeda BRL
        assertThat(codigo).contains("540580.50");         // valor com 2 casas
        assertThat(codigo).contains("5802BR");
        assertThat(codigo).contains("0510CUPCAKES42");    // txid
        assertThat(codigo).contains("6304");
    }

    @Test
    void crcFinalCorrespondeAoConteudo() {
        String codigo = pix.gerarCodigo(7L, new BigDecimal("12.90"));
        String semCrc = codigo.substring(0, codigo.length() - 4);
        assertThat(codigo.substring(codigo.length() - 4)).isEqualTo(PixService.crc16(semCrc));
    }

    @Test
    void nomeECidadeSemAcentoEComLimiteDeTamanho() {
        String codigo = pix.gerarCodigo(1L, BigDecimal.TEN);
        assertThat(codigo).contains("5925CUPCAKES GOURMET CONFEITA"); // 25 caracteres
        assertThat(codigo).contains("6009SAO PAULO");
    }

    @Test
    void geraQrCodePng() {
        byte[] png = pix.gerarQrCodePng(pix.gerarCodigo(1L, BigDecimal.ONE), 200);
        assertThat(png).hasSizeGreaterThan(100);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }
}
