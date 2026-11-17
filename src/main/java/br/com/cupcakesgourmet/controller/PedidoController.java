package br.com.cupcakesgourmet.controller;

import br.com.cupcakesgourmet.dto.CotacaoRequest;
import br.com.cupcakesgourmet.dto.CotacaoResponse;
import br.com.cupcakesgourmet.dto.PedidoRequest;
import br.com.cupcakesgourmet.dto.PedidoResponse;
import br.com.cupcakesgourmet.security.UsuarioLogado;
import br.com.cupcakesgourmet.service.PedidoService;
import br.com.cupcakesgourmet.service.PixService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** CU01, CU05, CU06, CU07, CU10. */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PixService pixService;

    public PedidoController(PedidoService pedidoService, PixService pixService) {
        this.pedidoService = pedidoService;
        this.pixService = pixService;
    }

    /** Pública: o carrinho pode ser cotado antes do login. */
    @PostMapping("/cotacao")
    public CotacaoResponse cotar(@Valid @RequestBody CotacaoRequest req) {
        return pedidoService.cotar(req.itens());
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest req,
                                                @AuthenticationPrincipal UsuarioLogado usuario) {
        PedidoResponse pedido = pedidoService.criarPedido(usuario.getId(), req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(pedido.id()).toUri();
        return ResponseEntity.created(location).body(pedido);
    }

    @GetMapping
    public List<PedidoResponse> meusPedidos(@AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.listarDoUsuario(usuario.getId());
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.buscar(id, usuario);
    }

    @GetMapping(value = "/{id}/pagamento/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado usuario) {
        byte[] png = pixService.gerarQrCodePng(pedidoService.codigoPix(id, usuario), 280);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(png);
    }

    @PostMapping("/{id}/pagamento/confirmar")
    public PedidoResponse confirmarPagamento(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.confirmarPagamento(id, usuario);
    }

    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.cancelar(id, usuario);
    }
}
