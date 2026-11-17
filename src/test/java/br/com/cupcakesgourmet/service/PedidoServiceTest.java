package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.dto.CotacaoResponse;
import br.com.cupcakesgourmet.dto.ItemPedidoRequest;
import br.com.cupcakesgourmet.dto.PedidoRequest;
import br.com.cupcakesgourmet.dto.PedidoResponse;
import br.com.cupcakesgourmet.exception.ConflitoException;
import br.com.cupcakesgourmet.exception.EstoqueInsuficienteException;
import br.com.cupcakesgourmet.exception.RecursoNaoEncontradoException;
import br.com.cupcakesgourmet.exception.RegraNegocioException;
import br.com.cupcakesgourmet.model.Cupcake;
import br.com.cupcakesgourmet.model.ItemPedido;
import br.com.cupcakesgourmet.model.MetodoPagamento;
import br.com.cupcakesgourmet.model.Pagamento;
import br.com.cupcakesgourmet.model.Papel;
import br.com.cupcakesgourmet.model.Pedido;
import br.com.cupcakesgourmet.model.StatusPagamento;
import br.com.cupcakesgourmet.model.StatusPedido;
import br.com.cupcakesgourmet.model.Usuario;
import br.com.cupcakesgourmet.repository.CupcakeRepository;
import br.com.cupcakesgourmet.repository.PedidoRepository;
import br.com.cupcakesgourmet.repository.UsuarioRepository;
import br.com.cupcakesgourmet.security.UsuarioLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService — regras do pedido")
class PedidoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T22:00:00Z");

    @Mock
    PedidoRepository pedidoRepository;
    @Mock
    CupcakeRepository cupcakeRepository;
    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    PixService pixService;

    PedidoService service;

    Usuario cliente;
    UsuarioLogado clienteLogado;
    UsuarioLogado outroCliente = new UsuarioLogado(99L, "Outro", "outro@x.com", "h", Papel.CLIENTE);
    UsuarioLogado admin = new UsuarioLogado(1L, "Admin", "admin@x.com", "h", Papel.ADMIN);

    Cupcake redVelvet;
    Cupcake pistache;

    @BeforeEach
    void setUp() {
        service = new PedidoService(pedidoRepository, cupcakeRepository, usuarioRepository, new CalculadoraFrete(),
                pixService, Clock.fixed(AGORA, ZoneOffset.UTC));
        cliente = new Usuario();
        cliente.setId(10L);
        cliente.setNome("Maria");
        cliente.setEmail("maria@x.com");
        clienteLogado = new UsuarioLogado(10L, "Maria", "maria@x.com", "h", Papel.CLIENTE);
        redVelvet = new Cupcake(1L, "Red Velvet", new BigDecimal("12.90"), 30);
        pistache = new Cupcake(5L, "Pistache", new BigDecimal("15.90"), 10);
    }

    private static PedidoRequest req(ItemPedidoRequest... itens) {
        return new PedidoRequest(List.of(itens), "Rua das Flores, 123");
    }

    private static ItemPedidoRequest item(Long id, int qtd) {
        return new ItemPedidoRequest(id, qtd);
    }

    private void prepararCheckout(Cupcake... cupcakes) {
        when(cupcakeRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(cupcakes));
        lenient().when(usuarioRepository.getReferenceById(10L)).thenReturn(cliente);
        lenient().when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });
        lenient().when(pixService.gerarCodigo(anyLong(), any())).thenReturn("000201...PIX");
    }

    @Nested
    @DisplayName("CU01 — criar pedido")
    class CriarPedido {

        @Test
        @DisplayName("cria pedido, baixa estoque, congela preço e calcula frete (RN01, RN02, RN04, RN05)")
        void criaPedidoComSucesso() {
            prepararCheckout(redVelvet, pistache);

            PedidoResponse resp = service.criarPedido(10L, req(item(1L, 3), item(5L, 2)));

            assertThat(resp.id()).isEqualTo(42L);
            assertThat(resp.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
            assertThat(resp.subtotal()).isEqualByComparingTo("70.50");
            assertThat(resp.frete()).isEqualByComparingTo("10.00");
            assertThat(resp.total()).isEqualByComparingTo("80.50");
            assertThat(resp.dataHora()).isEqualTo(AGORA);
            assertThat(resp.itens()).extracting(PedidoResponse.Item::sabor).containsExactly("Red Velvet", "Pistache");
            assertThat(resp.itens().get(0).precoUnitario()).isEqualByComparingTo("12.90");
            assertThat(resp.itens().get(0).subtotal()).isEqualByComparingTo("38.70");
            assertThat(redVelvet.getEstoque()).isEqualTo(27);
            assertThat(pistache.getEstoque()).isEqualTo(8);
            assertThat(resp.pagamento().metodo()).isEqualTo(MetodoPagamento.PIX);
            assertThat(resp.pagamento().status()).isEqualTo(StatusPagamento.PENDENTE);
            assertThat(resp.pagamento().valor()).isEqualByComparingTo("80.50");
            assertThat(resp.pagamento().codigoPix()).isEqualTo("000201...PIX");
            verify(pixService).gerarCodigo(42L, new BigDecimal("80.50"));
        }

        @Test
        @DisplayName("frete grátis quando subtotal >= R$ 100 (RN02)")
        void freteGratis() {
            prepararCheckout(pistache);
            PedidoResponse resp = service.criarPedido(10L, req(item(5L, 7))); // 111.30
            assertThat(resp.frete()).isEqualByComparingTo("0.00");
            assertThat(resp.total()).isEqualByComparingTo("111.30");
        }

        @Test
        @DisplayName("preço do item fica congelado mesmo se o cupcake mudar de preço depois (RN04)")
        void precoCongelado() {
            prepararCheckout(redVelvet);
            service.criarPedido(10L, req(item(1L, 2)));
            redVelvet.setPreco(new BigDecimal("99.00"));

            var captor = org.mockito.ArgumentCaptor.forClass(Pedido.class);
            verify(pedidoRepository).save(captor.capture());
            ItemPedido item = captor.getValue().getItens().get(0);
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo("12.90");
            assertThat(item.getSubtotal()).isEqualByComparingTo("25.80");
        }

        @Test
        @DisplayName("estoque insuficiente no checkout: lança 409 e não grava nada (RN01, RN05)")
        void estoqueInsuficiente() {
            prepararCheckout(redVelvet, pistache);

            assertThatThrownBy(() -> service.criarPedido(10L, req(item(1L, 2), item(5L, 11))))
                    .isInstanceOf(EstoqueInsuficienteException.class)
                    .hasMessage("Estoque insuficiente para Pistache: disponível 10, solicitado 11.");
            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("quantidades do mesmo cupcake são somadas (RN03)")
        void somaItensRepetidos() {
            prepararCheckout(redVelvet);
            PedidoResponse resp = service.criarPedido(10L, req(item(1L, 2), item(1L, 3)));
            assertThat(resp.itens()).hasSize(1);
            assertThat(resp.itens().get(0).quantidade()).isEqualTo(5);
            assertThat(redVelvet.getEstoque()).isEqualTo(25);
        }

        @Test
        @DisplayName("soma acima de 20 unidades do mesmo cupcake é rejeitada (RN03)")
        void limitePorItem() {
            assertThatThrownBy(() -> service.criarPedido(10L, req(item(1L, 15), item(1L, 6))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("máxima por item é 20");
        }

        @Test
        @DisplayName("carrinho vazio é rejeitado")
        void carrinhoVazio() {
            assertThatThrownBy(() -> service.criarPedido(10L, new PedidoRequest(List.of(), "Rua X")))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Seu carrinho está vazio.");
        }

        @Test
        @DisplayName("cupcake desativado não pode ser comprado (RN10)")
        void cupcakeInativo() {
            pistache.setAtivo(false);
            prepararCheckout(pistache);
            assertThatThrownBy(() -> service.criarPedido(10L, req(item(5L, 1))))
                    .isInstanceOf(ConflitoException.class)
                    .hasMessage("O cupcake Pistache não está mais disponível.");
        }

        @Test
        @DisplayName("cupcake inexistente gera 404")
        void cupcakeInexistente() {
            prepararCheckout();
            assertThatThrownBy(() -> service.criarPedido(10L, req(item(777L, 1))))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Test
    @DisplayName("cotação calcula subtotal, frete e total sem baixar estoque")
    void cotacao() {
        when(cupcakeRepository.findAllById(anyCollection())).thenReturn(List.of(redVelvet));
        CotacaoResponse cot = service.cotar(List.of(item(1L, 2)));
        assertThat(cot.subtotal()).isEqualByComparingTo("25.80");
        assertThat(cot.frete()).isEqualByComparingTo("10.00");
        assertThat(cot.total()).isEqualByComparingTo("35.80");
        assertThat(cot.itens().get(0).estoqueDisponivel()).isEqualTo(30);
        assertThat(redVelvet.getEstoque()).isEqualTo(30);
    }

    private Pedido pedidoExistente(StatusPedido status) {
        Pedido p = new Pedido();
        p.setId(42L);
        p.setUsuario(cliente);
        p.setStatus(status);
        p.setDataHora(AGORA);
        p.setEnderecoEntrega("Rua X");
        p.setSubtotal(new BigDecimal("25.80"));
        p.setFrete(new BigDecimal("10.00"));
        p.setTotal(new BigDecimal("35.80"));
        p.adicionarItem(new ItemPedido(redVelvet, 2, new BigDecimal("12.90"), new BigDecimal("25.80")));
        Pagamento pg = new Pagamento();
        pg.setMetodo(MetodoPagamento.PIX);
        pg.setStatus(status == StatusPedido.AGUARDANDO_PAGAMENTO ? StatusPagamento.PENDENTE : StatusPagamento.CONFIRMADO);
        pg.setValor(p.getTotal());
        p.definirPagamento(pg);
        when(pedidoRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(p));
        return p;
    }

    @Nested
    @DisplayName("CU07 — confirmar pagamento (RN09)")
    class ConfirmarPagamento {

        @Test
        void confirmaPedidoAguardandoPagamento() {
            pedidoExistente(StatusPedido.AGUARDANDO_PAGAMENTO);
            PedidoResponse resp = service.confirmarPagamento(42L, clienteLogado);
            assertThat(resp.status()).isEqualTo(StatusPedido.PAGO);
            assertThat(resp.pagamento().status()).isEqualTo(StatusPagamento.CONFIRMADO);
            assertThat(resp.pagamento().confirmadoEm()).isEqualTo(AGORA);
        }

        @Test
        void naoConfirmaDuasVezes() {
            pedidoExistente(StatusPedido.PAGO);
            assertThatThrownBy(() -> service.confirmarPagamento(42L, clienteLogado))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Não é possível mudar o pedido de PAGO para PAGO.");
        }

        @Test
        void clienteNaoAcessaPedidoDeOutro() {
            pedidoExistente(StatusPedido.AGUARDANDO_PAGAMENTO);
            assertThatThrownBy(() -> service.confirmarPagamento(42L, outroCliente))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Pedido não encontrado.");
        }
    }

    @Nested
    @DisplayName("CU10 — cancelar pedido (RN07, RN08)")
    class Cancelar {

        @Test
        void clienteCancelaEEstoqueVolta() {
            pedidoExistente(StatusPedido.AGUARDANDO_PAGAMENTO);
            when(cupcakeRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(redVelvet));

            PedidoResponse resp = service.cancelar(42L, clienteLogado);

            assertThat(resp.status()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(resp.pagamento().status()).isEqualTo(StatusPagamento.CANCELADO);
            assertThat(redVelvet.getEstoque()).isEqualTo(32);
        }

        @Test
        void clienteNaoCancelaPedidoPago() {
            pedidoExistente(StatusPedido.PAGO);
            assertThatThrownBy(() -> service.cancelar(42L, clienteLogado))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("só podem ser cancelados pela loja");
            assertThat(redVelvet.getEstoque()).isEqualTo(30);
        }

        @Test
        void adminCancelaPedidoPago() {
            pedidoExistente(StatusPedido.PAGO);
            when(cupcakeRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(redVelvet));
            assertThat(service.cancelar(42L, admin).status()).isEqualTo(StatusPedido.CANCELADO);
            assertThat(redVelvet.getEstoque()).isEqualTo(32);
        }

        @Test
        void naoCancelaPedidoEnviado() {
            pedidoExistente(StatusPedido.ENVIADO);
            assertThatThrownBy(() -> service.cancelar(42L, admin)).isInstanceOf(RegraNegocioException.class);
        }
    }

    @Nested
    @DisplayName("CU09 — admin atualiza status (RN07)")
    class AtualizarStatus {

        @Test
        void avancaDePagoParaEmPreparo() {
            pedidoExistente(StatusPedido.PAGO);
            assertThat(service.atualizarStatus(42L, StatusPedido.EM_PREPARO, admin).status())
                    .isEqualTo(StatusPedido.EM_PREPARO);
        }

        @Test
        void naoPulaEtapas() {
            pedidoExistente(StatusPedido.AGUARDANDO_PAGAMENTO);
            assertThatThrownBy(() -> service.atualizarStatus(42L, StatusPedido.ENVIADO, admin))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Não é possível mudar o pedido de AGUARDANDO_PAGAMENTO para ENVIADO.");
        }

        @Test
        void marcarComoPagoConfirmaOPagamento() {
            pedidoExistente(StatusPedido.AGUARDANDO_PAGAMENTO);
            PedidoResponse resp = service.atualizarStatus(42L, StatusPedido.PAGO, admin);
            assertThat(resp.pagamento().status()).isEqualTo(StatusPagamento.CONFIRMADO);
        }
    }

    @Test
    @DisplayName("buscar pedido de outro cliente retorna 404 (RN10)")
    void buscarPedidoDeOutroCliente() {
        Pedido p = new Pedido();
        p.setUsuario(cliente);
        when(pedidoRepository.findComDetalhesById(42L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.buscar(42L, outroCliente)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
