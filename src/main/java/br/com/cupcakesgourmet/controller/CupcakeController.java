package br.com.cupcakesgourmet.controller;

import br.com.cupcakesgourmet.dto.CupcakeResponse;
import br.com.cupcakesgourmet.service.CupcakeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CU02 — Vitrine pública. */
@RestController
@RequestMapping("/api/cupcakes")
public class CupcakeController {

    private final CupcakeService service;

    public CupcakeController(CupcakeService service) {
        this.service = service;
    }

    @GetMapping
    public List<CupcakeResponse> listar() {
        return service.listarVitrine();
    }

    @GetMapping("/{id}")
    public CupcakeResponse buscar(@PathVariable Long id) {
        return service.buscarNaVitrine(id);
    }
}
