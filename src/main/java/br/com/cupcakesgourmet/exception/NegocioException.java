package br.com.cupcakesgourmet.exception;

import org.springframework.http.HttpStatus;

/** Base das exceções de negócio: carrega o status HTTP e o título exibidos no ProblemDetail. */
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String titulo;

    protected NegocioException(HttpStatus status, String titulo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.titulo = titulo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitulo() {
        return titulo;
    }
}
