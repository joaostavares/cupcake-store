package br.com.cupcakesgourmet.repository;

import br.com.cupcakesgourmet.model.Pedido;
import br.com.cupcakesgourmet.model.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.cupcake", "pagamento"})
    List<Pedido> findByUsuarioIdOrderByDataHoraDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.cupcake", "pagamento"})
    List<Pedido> findAllByOrderByDataHoraDesc();

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.cupcake", "pagamento"})
    List<Pedido> findByStatusOrderByDataHoraDesc(StatusPedido status);

    @EntityGraph(attributePaths = {"usuario", "itens", "itens.cupcake", "pagamento"})
    Optional<Pedido> findComDetalhesById(Long id);

    /** Bloqueia o pedido para evitar confirmação e cancelamento simultâneos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}
