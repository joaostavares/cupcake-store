package br.com.cupcakesgourmet.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN07 — Máquina de estados do pedido")
class StatusPedidoTest {

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "AGUARDANDO_PAGAMENTO, PAGO, true",
            "AGUARDANDO_PAGAMENTO, CANCELADO, true",
            "AGUARDANDO_PAGAMENTO, EM_PREPARO, false",
            "AGUARDANDO_PAGAMENTO, ENTREGUE, false",
            "PAGO, EM_PREPARO, true",
            "PAGO, CANCELADO, true",
            "PAGO, AGUARDANDO_PAGAMENTO, false",
            "EM_PREPARO, ENVIADO, true",
            "EM_PREPARO, CANCELADO, false",
            "ENVIADO, ENTREGUE, true",
            "ENVIADO, EM_PREPARO, false",
            "ENTREGUE, CANCELADO, false",
            "CANCELADO, PAGO, false",
            "CANCELADO, AGUARDANDO_PAGAMENTO, false"
    })
    void validaTransicoes(StatusPedido atual, StatusPedido novo, boolean permitido) {
        assertThat(atual.podeMudarPara(novo)).isEqualTo(permitido);
    }
}
