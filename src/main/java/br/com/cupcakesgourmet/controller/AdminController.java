package br.com.cupcakesgourmet.controller;

import br.com.cupcakesgourmet.dto.CupcakeRequest;
import br.com.cupcakesgourmet.dto.CupcakeResponse;
import br.com.cupcakesgourmet.dto.EstoqueRequest;
import br.com.cupcakesgourmet.dto.PedidoResponse;
import br.com.cupcakesgourmet.dto.StatusRequest;
import br.com.cupcakesgourmet.model.StatusPedido;
import br.com.cupcakesgourmet.security.UsuarioLogado;
import br.com.cupcakesgourmet.service.CupcakeService;
import br.com.cupcakesgourmet.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CU08 e CU09 — exclusivo do papel ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CupcakeService cupcakeService;
    private final PedidoService pedidoService;

    public AdminController(CupcakeService cupcakeService, PedidoService pedidoService) {
        this.cupcakeService = cupcakeService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/cupcakes")
    public List<CupcakeResponse> listarCupcakes() {
        return cupcakeService.listarTodos();
    }

    @PostMapping("/cupcakes")
    @ResponseStatus(HttpStatus.CREATED)
    public CupcakeResponse criarCupcake(@Valid @RequestBody CupcakeRequest req) {
        return cupcakeService.criar(req);
    }

    @PutMapping("/cupcakes/{id}")
    public CupcakeResponse atualizarCupcake(@PathVariable Long id, @Valid @RequestBody CupcakeRequest req) {
        return cupcakeService.atualizar(id, req);
    }

    @PatchMapping("/cupcakes/{id}/estoque")
    public CupcakeResponse definirEstoque(@PathVariable Long id, @Valid @RequestBody EstoqueRequest req) {
        return cupcakeService.definirEstoque(id, req.estoque());
    }

    @GetMapping("/pedidos")
    public List<PedidoResponse> listarPedidos(@RequestParam(required = false) StatusPedido status) {
        return pedidoService.listarTodos(status);
    }

    @PatchMapping("/pedidos/{id}/status")
    public PedidoResponse atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req,
                                          @AuthenticationPrincipal UsuarioLogado admin) {
        return pedidoService.atualizarStatus(id, req.status(), admin);
    }
}
