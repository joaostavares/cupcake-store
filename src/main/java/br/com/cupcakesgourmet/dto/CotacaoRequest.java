package br.com.cupcakesgourmet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CotacaoRequest(
        @NotEmpty(message = "Seu carrinho está vazio.")
        @Size(max = 50, message = "O pedido pode ter no máximo 50 itens.")
        List<@Valid ItemPedidoRequest> itens) {
}
