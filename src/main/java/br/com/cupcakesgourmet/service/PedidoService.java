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
import br.com.cupcakesgourmet.model.Pedido;
import br.com.cupcakesgourmet.model.StatusPagamento;
import br.com.cupcakesgourmet.model.StatusPedido;
import br.com.cupcakesgourmet.repository.CupcakeRepository;
import br.com.cupcakesgourmet.repository.PedidoRepository;
import br.com.cupcakesgourmet.repository.UsuarioRepository;
import br.com.cupcakesgourmet.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Regras do pedido: RN01–RN05, RN07–RN10. */
@Service
public class PedidoService {

    static final int QUANTIDADE_MAXIMA_POR_ITEM = 20;

    private final PedidoRepository pedidoRepository;
    private final CupcakeRepository cupcakeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculadoraFrete calculadoraFrete;
    private final PixService pixService;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidoRepository, CupcakeRepository cupcakeRepository,
                         UsuarioRepository usuarioRepository, CalculadoraFrete calculadoraFrete,
                         PixService pixService, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.cupcakeRepository = cupcakeRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculadoraFrete = calculadoraFrete;
        this.pixService = pixService;
        this.clock = clock;
    }

    /** Prévia de valores do carrinho (RN02 calculada no servidor). Não reserva estoque. */
    @Transactional(readOnly = true)
    public CotacaoResponse cotar(List<ItemPedidoRequest> itens) {
        Map<Long, Integer> quantidades = consolidar(itens);
        Map<Long, Cupcake> cupcakes = cupcakeRepository.findAllById(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Cupcake::getId, Function.identity()));

        List<CotacaoResponse.Item> linhas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var entry : quantidades.entrySet()) {
            Cupcake c = exigirDisponivel(cupcakes.get(entry.getKey()), entry.getKey());
            BigDecimal sub = c.getPreco().multiply(BigDecimal.valueOf(entry.getValue()));
            subtotal = subtotal.add(sub);
            linhas.add(new CotacaoResponse.Item(c.getId(), c.getSabor(), c.getPreco(), entry.getValue(), sub, c.getEstoque()));
        }
        BigDecimal frete = calculadoraFrete.calcularFrete(subtotal);
        return new CotacaoResponse(linhas, subtotal, frete, subtotal.add(frete));
    }

    /**
     * CU01 — Finalizar compra. Tudo na mesma transação: bloqueia os cupcakes, revalida o estoque (RN01/RN05),
     * congela os preços (RN04), baixa o estoque, calcula o frete (RN02) e gera o pagamento PIX (RN09).
     */
    @Transactional
    public PedidoResponse criarPedido(Long usuarioId, PedidoRequest req) {
        Map<Long, Integer> quantidades = consolidar(req.itens());
        Map<Long, Cupcake> cupcakes = cupcakeRepository.findAllByIdForUpdate(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Cupcake::getId, Function.identity()));

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        pedido.setDataHora(Instant.now(clock));
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setEnderecoEntrega(req.enderecoEntrega().trim());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (var entry : quantidades.entrySet()) {
            Cupcake c = exigirDisponivel(cupcakes.get(entry.getKey()), entry.getKey());
            int quantidade = entry.getValue();
            if (c.getEstoque() < quantidade) {
                throw new EstoqueInsuficienteException(c.getId(), c.getSabor(), c.getEstoque(), quantidade);
            }
            BigDecimal sub = c.getPreco().multiply(BigDecimal.valueOf(quantidade));
            pedido.adicionarItem(new ItemPedido(c, quantidade, c.getPreco(), sub));
            c.setEstoque(c.getEstoque() - quantidade);
            subtotal = subtotal.add(sub);
        }

        BigDecimal frete = calculadoraFrete.calcularFrete(subtotal);
        pedido.setSubtotal(subtotal);
        pedido.setFrete(frete);
        pedido.setTotal(subtotal.add(frete));
        pedido = pedidoRepository.save(pedido);

        Pagamento pagamento = new Pagamento();
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValor(pedido.getTotal());
        pagamento.setCriadoEm(Instant.now(clock));
        pagamento.setCodigoPix(pixService.gerarCodigo(pedido.getId(), pedido.getTotal()));
        pedido.definirPagamento(pagamento);

        return PedidoResponse.de(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarDoUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByDataHoraDesc(usuarioId).stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos(StatusPedido status) {
        List<Pedido> pedidos = status == null
                ? pedidoRepository.findAllByOrderByDataHoraDesc()
                : pedidoRepository.findByStatusOrderByDataHoraDesc(status);
        return pedidos.stream().map(PedidoResponse::de).toList();
    }

    /** RN10 — cliente só enxerga os próprios pedidos (404 para não revelar que o pedido existe). */
    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long pedidoId, UsuarioLogado ator) {
        Pedido pedido = pedidoRepository.findComDetalhesById(pedidoId)
                .filter(p -> podeAcessar(p, ator))
                .orElseThrow(PedidoService::pedidoNaoEncontrado);
        return PedidoResponse.de(pedido);
    }

    @Transactional(readOnly = true)
    public String codigoPix(Long pedidoId, UsuarioLogado ator) {
        PedidoResponse pedido = buscar(pedidoId, ator);
        if (pedido.pagamento() == null || pedido.pagamento().codigoPix() == null) {
            throw new RecursoNaoEncontradoException("Pagamento não encontrado.");
        }
        return pedido.pagamento().codigoPix();
    }

    /** CU07 / RN09 — confirmação do pagamento (no MVP, simula o webhook do PSP). */
    @Transactional
    public PedidoResponse confirmarPagamento(Long pedidoId, UsuarioLogado ator) {
        Pedido pedido = buscarParaAlterar(pedidoId, ator);
        exigirTransicao(pedido, StatusPedido.PAGO);
        pedido.setStatus(StatusPedido.PAGO);
        pedido.getPagamento().setStatus(StatusPagamento.CONFIRMADO);
        pedido.getPagamento().setConfirmadoEm(Instant.now(clock));
        return PedidoResponse.de(pedido);
    }

    /** CU10 / RN07 / RN08 — cancela e devolve o estoque. */
    @Transactional
    public PedidoResponse cancelar(Long pedidoId, UsuarioLogado ator) {
        Pedido pedido = buscarParaAlterar(pedidoId, ator);
        exigirTransicao(pedido, StatusPedido.CANCELADO);
        if (!ator.isAdmin() && pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("Pedidos já pagos só podem ser cancelados pela loja. Entre em contato.");
        }

        List<Long> ids = pedido.getItens().stream().map(i -> i.getCupcake().getId()).toList();
        Map<Long, Cupcake> cupcakes = cupcakeRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Cupcake::getId, Function.identity()));
        for (ItemPedido item : pedido.getItens()) {
            Cupcake c = cupcakes.get(item.getCupcake().getId());
            c.setEstoque(c.getEstoque() + item.getQuantidade());
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.getPagamento().setStatus(StatusPagamento.CANCELADO);
        return PedidoResponse.de(pedido);
    }

    /** CU09 — admin movimenta o pedido seguindo a máquina de estados (RN07). */
    @Transactional
    public PedidoResponse atualizarStatus(Long pedidoId, StatusPedido novo, UsuarioLogado admin) {
        return switch (novo) {
            case PAGO -> confirmarPagamento(pedidoId, admin);
            case CANCELADO -> cancelar(pedidoId, admin);
            default -> {
                Pedido pedido = buscarParaAlterar(pedidoId, admin);
                exigirTransicao(pedido, novo);
                pedido.setStatus(novo);
                yield PedidoResponse.de(pedido);
            }
        };
    }

    /** RN03 — soma quantidades do mesmo cupcake e aplica o limite por item. */
    static Map<Long, Integer> consolidar(List<ItemPedidoRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("Seu carrinho está vazio.");
        }
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : itens) {
            quantidades.merge(item.cupcakeId(), item.quantidade(), Integer::sum);
        }
        quantidades.forEach((id, qtd) -> {
            if (qtd > QUANTIDADE_MAXIMA_POR_ITEM) {
                throw new RegraNegocioException("A quantidade máxima por item é %d.".formatted(QUANTIDADE_MAXIMA_POR_ITEM));
            }
        });
        return quantidades;
    }

    private static Cupcake exigirDisponivel(Cupcake cupcake, Long id) {
        if (cupcake == null) {
            throw new RecursoNaoEncontradoException("Cupcake %d não encontrado.".formatted(id));
        }
        if (!cupcake.isAtivo()) {
            throw new ConflitoException("O cupcake %s não está mais disponível.".formatted(cupcake.getSabor()));
        }
        return cupcake;
    }

    private Pedido buscarParaAlterar(Long pedidoId, UsuarioLogado ator) {
        return pedidoRepository.findByIdForUpdate(pedidoId)
                .filter(p -> podeAcessar(p, ator))
                .orElseThrow(PedidoService::pedidoNaoEncontrado);
    }

    private static boolean podeAcessar(Pedido pedido, UsuarioLogado ator) {
        return ator.isAdmin() || pedido.getUsuario().getId().equals(ator.getId());
    }

    private static void exigirTransicao(Pedido pedido, StatusPedido novo) {
        if (!pedido.getStatus().podeMudarPara(novo)) {
            throw new RegraNegocioException("Não é possível mudar o pedido de %s para %s."
                    .formatted(pedido.getStatus(), novo));
        }
    }

    private static RecursoNaoEncontradoException pedidoNaoEncontrado() {
        return new RecursoNaoEncontradoException("Pedido não encontrado.");
    }
}
