package br.com.cupcakesgourmet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        @NotEmpty(message = "Seu carrinho está vazio.")
        @Size(max = 50, message = "O pedido pode ter no máximo 50 itens.")
        List<@Valid ItemPedidoRequest> itens,

        @NotBlank(message = "Informe o endereço de entrega.")
        @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres.")
        String enderecoEntrega) {
}
