package br.com.cupcakesgourmet.dto;

import br.com.cupcakesgourmet.model.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "Informe o novo status.") StatusPedido status) {
}
