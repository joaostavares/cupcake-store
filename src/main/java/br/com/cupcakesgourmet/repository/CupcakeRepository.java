package br.com.cupcakesgourmet.repository;

import br.com.cupcakesgourmet.model.Cupcake;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CupcakeRepository extends JpaRepository<Cupcake, Long> {

    List<Cupcake> findByAtivoTrueOrderBySaborAsc();

    List<Cupcake> findAllByOrderBySaborAsc();

    Optional<Cupcake> findByIdAndAtivoTrue(Long id);

    boolean existsBySaborIgnoreCase(String sabor);

    boolean existsBySaborIgnoreCaseAndIdNot(String sabor, Long id);

    /**
     * Bloqueia as linhas (SELECT ... FOR UPDATE) para revalidar e baixar o estoque na transação do
     * checkout (RN05). Ordenado por id para que transações concorrentes travem na mesma ordem
     * e não entrem em deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cupcake c where c.id in :ids order by c.id")
    List<Cupcake> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
