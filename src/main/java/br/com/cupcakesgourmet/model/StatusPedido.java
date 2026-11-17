package br.com.cupcakesgourmet.model;

import java.util.Map;
import java.util.Set;

/**
 * Status do pedido e suas transições válidas (RN07).
 * A tabela de transições é dado de domínio; quem decide e executa a mudança é o PedidoService.
 */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    EM_PREPARO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES = Map.of(
            AGUARDANDO_PAGAMENTO, Set.of(PAGO, CANCELADO),
            PAGO, Set.of(EM_PREPARO, CANCELADO),
            EM_PREPARO, Set.of(ENVIADO),
            ENVIADO, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADO, Set.of());

    public boolean podeMudarPara(StatusPedido novo) {
        return TRANSICOES.get(this).contains(novo);
    }
}
