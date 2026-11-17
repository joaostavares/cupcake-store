package br.com.cupcakesgourmet.exception;

import org.springframework.http.HttpStatus;

/** Operação válida sintaticamente, mas proibida pelas regras de negócio (HTTP 422). */
public class RegraNegocioException extends NegocioException {

    public RegraNegocioException(String mensagem) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Operação não permitida", mensagem);
    }
}
