package br.com.cupcakesgourmet.api;

import br.com.cupcakesgourmet.dto.ItemPedidoRequest;
import br.com.cupcakesgourmet.dto.PedidoRequest;
import br.com.cupcakesgourmet.exception.EstoqueInsuficienteException;
import br.com.cupcakesgourmet.model.Cupcake;
import br.com.cupcakesgourmet.repository.CupcakeRepository;
import br.com.cupcakesgourmet.repository.UsuarioRepository;
import br.com.cupcakesgourmet.service.PedidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RN05 — 10 clientes tentam comprar ao mesmo tempo o último lote de 5 unidades.
 * Sem o bloqueio pessimista, duas transações poderiam ler o mesmo estoque e vender mais do que existe.
 */
abstract class CheckoutConcorrenciaTestBase {

    @Autowired
    PedidoService pedidoService;
    @Autowired
    CupcakeRepository cupcakeRepository;
    @Autowired
    UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("RN05 — checkouts simultâneos nunca vendem além do estoque")
    void naoVendeAlemDoEstoque() throws Exception {
        Cupcake c = new Cupcake();
        c.setSabor("Concorrência " + UUID.randomUUID().toString().substring(0, 8));
        c.setPreco(new BigDecimal("10.00"));
        c.setEstoque(5);
        long cupcakeId = cupcakeRepository.save(c).getId();
        long usuarioId = usuarioRepository.findByEmail("cliente@cupcakes.com").orElseThrow().getId();
        PedidoRequest req = new PedidoRequest(List.of(new ItemPedidoRequest(cupcakeId, 1)), "Rua X");

        int tentativas = 10;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            resultados.add(pool.submit(() -> {
                largada.await();
                try {
                    pedidoService.criarPedido(usuarioId, req);
                    return true;
                } catch (EstoqueInsuficienteException e) {
                    return false;
                }
            }));
        }
        largada.countDown();

        int vendidos = 0;
        for (Future<Boolean> r : resultados) {
            if (r.get()) {
                vendidos++;
            }
        }
        pool.shutdown();

        assertThat(vendidos).isEqualTo(5);
        assertThat(cupcakeRepository.findById(cupcakeId).orElseThrow().getEstoque()).isZero();
    }
}
