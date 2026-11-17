package br.com.cupcakesgourmet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "Informe o cupcake.")
        Long cupcakeId,

        @NotNull(message = "Informe a quantidade.")
        @Min(value = 1, message = "A quantidade mínima é 1.")
        @Max(value = 20, message = "A quantidade máxima por item é 20.")
        Integer quantidade) {
}
