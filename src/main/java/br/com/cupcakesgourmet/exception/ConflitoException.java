package br.com.cupcakesgourmet.exception;

import org.springframework.http.HttpStatus;

public class ConflitoException extends NegocioException {

    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, "Conflito", mensagem);
    }

    protected ConflitoException(String titulo, String mensagem) {
        super(HttpStatus.CONFLICT, titulo, mensagem);
    }
}
