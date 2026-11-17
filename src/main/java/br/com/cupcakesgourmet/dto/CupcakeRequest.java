package br.com.cupcakesgourmet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CupcakeRequest(
        @NotBlank(message = "Informe o sabor.")
        @Size(max = 80, message = "O sabor deve ter no máximo 80 caracteres.")
        String sabor,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @NotNull(message = "Informe o preço.")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Preço inválido (máximo 2 casas decimais).")
        BigDecimal preco,

        @NotNull(message = "Informe o estoque.")
        @Min(value = 0, message = "O estoque não pode ser negativo.")
        @Max(value = 100000, message = "Estoque máximo é 100000.")
        Integer estoque,

        @Size(max = 255, message = "A URL da imagem deve ter no máximo 255 caracteres.")
        String imagemUrl,

        Boolean ativo) {
}
