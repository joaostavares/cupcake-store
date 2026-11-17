package br.com.cupcakesgourmet.exception;

/** RN01/RN05 — MSG05. */
public class EstoqueInsuficienteException extends ConflitoException {

    private final Long cupcakeId;
    private final int disponivel;
    private final int solicitado;

    public EstoqueInsuficienteException(Long cupcakeId, String sabor, int disponivel, int solicitado) {
        super("Estoque insuficiente", "Estoque insuficiente para %s: disponível %d, solicitado %d."
                .formatted(sabor, disponivel, solicitado));
        this.cupcakeId = cupcakeId;
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    public Long getCupcakeId() { return cupcakeId; }
    public int getDisponivel() { return disponivel; }
    public int getSolicitado() { return solicitado; }
}
