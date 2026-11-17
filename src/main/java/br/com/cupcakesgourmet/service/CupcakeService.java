package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.dto.CupcakeRequest;
import br.com.cupcakesgourmet.dto.CupcakeResponse;
import br.com.cupcakesgourmet.exception.ConflitoException;
import br.com.cupcakesgourmet.exception.RecursoNaoEncontradoException;
import br.com.cupcakesgourmet.model.Cupcake;
import br.com.cupcakesgourmet.repository.CupcakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CupcakeService {

    private final CupcakeRepository repository;

    public CupcakeService(CupcakeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CupcakeResponse> listarVitrine() {
        return repository.findByAtivoTrueOrderBySaborAsc().stream().map(CupcakeResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CupcakeResponse buscarNaVitrine(Long id) {
        return repository.findByIdAndAtivoTrue(id).map(CupcakeResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<CupcakeResponse> listarTodos() {
        return repository.findAllByOrderBySaborAsc().stream().map(CupcakeResponse::de).toList();
    }

    @Transactional
    public CupcakeResponse criar(CupcakeRequest req) {
        String sabor = req.sabor().trim();
        if (repository.existsBySaborIgnoreCase(sabor)) {
            throw new ConflitoException("Já existe um cupcake com o sabor '%s'.".formatted(sabor));
        }
        Cupcake cupcake = new Cupcake();
        preencher(cupcake, req, sabor);
        return CupcakeResponse.de(repository.save(cupcake));
    }

    @Transactional
    public CupcakeResponse atualizar(Long id, CupcakeRequest req) {
        Cupcake cupcake = buscar(id);
        String sabor = req.sabor().trim();
        if (repository.existsBySaborIgnoreCaseAndIdNot(sabor, id)) {
            throw new ConflitoException("Já existe um cupcake com o sabor '%s'.".formatted(sabor));
        }
        preencher(cupcake, req, sabor);
        return CupcakeResponse.de(cupcake);
    }

    @Transactional
    public CupcakeResponse definirEstoque(Long id, int estoque) {
        Cupcake cupcake = repository.findAllByIdForUpdate(List.of(id)).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado."));
        cupcake.setEstoque(estoque);
        return CupcakeResponse.de(cupcake);
    }

    private Cupcake buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado."));
    }

    private static void preencher(Cupcake cupcake, CupcakeRequest req, String sabor) {
        cupcake.setSabor(sabor);
        cupcake.setDescricao(vazioParaNulo(req.descricao()));
        cupcake.setPreco(req.preco());
        cupcake.setEstoque(req.estoque());
        cupcake.setImagemUrl(vazioParaNulo(req.imagemUrl()));
        cupcake.setAtivo(req.ativo() == null || req.ativo());
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
