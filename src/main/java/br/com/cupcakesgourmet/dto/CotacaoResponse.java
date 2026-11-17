package br.com.cupcakesgourmet.dto;

import java.math.BigDecimal;
import java.util.List;

public record CotacaoResponse(List<Item> itens, BigDecimal subtotal, BigDecimal frete, BigDecimal total) {

    public record Item(Long cupcakeId, String sabor, BigDecimal precoUnitario, Integer quantidade,
                       BigDecimal subtotal, Integer estoqueDisponivel) {
    }
}
