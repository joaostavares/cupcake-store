package br.com.cupcakesgourmet.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Padroniza todas as respostas de erro da API como ProblemDetail (RFC 9457). Ver docs/situacao-1/ihc/mensagens.md. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    ProblemDetail negocio(NegocioException e) {
        ProblemDetail problem = problema(e.getStatus(), e.getTitulo(), e.getMessage());
        if (e instanceof EstoqueInsuficienteException est) {
            problem.setProperty("cupcakeId", est.getCupcakeId());
            problem.setProperty("disponivel", est.getDisponivel());
            problem.setProperty("solicitado", est.getSolicitado());
        }
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail credenciais() {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", "E-mail ou senha inválidos.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Valor inválido para '%s'.".formatted(e.getName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito", "A operação viola uma regra de consistência dos dados.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail concorrencia() {
        return problema(HttpStatus.CONFLICT, "Conflito", "Outra operação está usando estes dados. Tente novamente.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente em instantes.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(ge -> erros.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ProblemDetail problem = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", resumo(erros));
        problem.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        return ResponseEntity.badRequest()
                .body(problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "O corpo da requisição está mal formatado."));
    }

    /** Se só há um erro (ex.: carrinho vazio), ele vira o detail; senão a mensagem genérica MSG03. */
    private static String resumo(Map<String, String> erros) {
        return erros.size() == 1 ? erros.values().iterator().next() : "Verifique os campos destacados.";
    }

    private static ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setTitle(titulo);
        return problem;
    }
}
