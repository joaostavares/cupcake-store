package br.com.cupcakesgourmet.dto;

import br.com.cupcakesgourmet.model.ItemPedido;
import br.com.cupcakesgourmet.model.MetodoPagamento;
import br.com.cupcakesgourmet.model.Pagamento;
import br.com.cupcakesgourmet.model.Pedido;
import br.com.cupcakesgourmet.model.StatusPagamento;
import br.com.cupcakesgourmet.model.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(Long id, Instant dataHora, StatusPedido status, BigDecimal subtotal, BigDecimal frete,
                             BigDecimal total, String enderecoEntrega, String clienteNome, String clienteEmail,
                             List<Item> itens, PagamentoInfo pagamento) {

    public record Item(Long cupcakeId, String sabor, Integer quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {

        static Item de(ItemPedido i) {
            return new Item(i.getCupcake().getId(), i.getCupcake().getSabor(), i.getQuantidade(),
                    i.getPrecoUnitario(), i.getSubtotal());
        }
    }

    public record PagamentoInfo(MetodoPagamento metodo, StatusPagamento status, BigDecimal valor, String codigoPix,
                                Instant criadoEm, Instant confirmadoEm) {

        static PagamentoInfo de(Pagamento p) {
            return p == null ? null : new PagamentoInfo(p.getMetodo(), p.getStatus(), p.getValor(), p.getCodigoPix(),
                    p.getCriadoEm(), p.getConfirmadoEm());
        }
    }

    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(p.getId(), p.getDataHora(), p.getStatus(), p.getSubtotal(), p.getFrete(), p.getTotal(),
                p.getEnderecoEntrega(), p.getUsuario().getNome(), p.getUsuario().getEmail(),
                p.getItens().stream().map(Item::de).toList(), PagamentoInfo.de(p.getPagamento()));
    }
}
