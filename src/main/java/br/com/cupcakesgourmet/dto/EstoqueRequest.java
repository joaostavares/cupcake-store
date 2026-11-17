package br.com.cupcakesgourmet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EstoqueRequest(
        @NotNull(message = "Informe o estoque.")
        @Min(value = 0, message = "O estoque não pode ser negativo.")
        @Max(value = 100000, message = "Estoque máximo é 100000.")
        Integer estoque) {
}
