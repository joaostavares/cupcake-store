package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.dto.CupcakeRequest;
import br.com.cupcakesgourmet.dto.CupcakeResponse;
import br.com.cupcakesgourmet.exception.ConflitoException;
import br.com.cupcakesgourmet.exception.RecursoNaoEncontradoException;
import br.com.cupcakesgourmet.model.Cupcake;
import br.com.cupcakesgourmet.repository.CupcakeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CU08 — Gerenciar catálogo e estoque")
class CupcakeServiceTest {

    @Mock
    CupcakeRepository repository;

    @InjectMocks
    CupcakeService service;

    private static CupcakeRequest req(String sabor) {
        return new CupcakeRequest(sabor, "  ", new BigDecimal("9.90"), 5, "", null);
    }

    @Test
    void criaCupcakeAtivoPorPadrao() {
        when(repository.existsBySaborIgnoreCase("Morango")).thenReturn(false);
        when(repository.save(any(Cupcake.class))).thenAnswer(inv -> inv.getArgument(0));

        CupcakeResponse resp = service.criar(req("  Morango "));

        assertThat(resp.sabor()).isEqualTo("Morango");
        assertThat(resp.ativo()).isTrue();
        assertThat(resp.descricao()).isNull();
        assertThat(resp.imagemUrl()).isNull();
    }

    @Test
    void rejeitaSaborDuplicado() {
        when(repository.existsBySaborIgnoreCase("Red Velvet")).thenReturn(true);
        assertThatThrownBy(() -> service.criar(req("Red Velvet")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um cupcake com o sabor 'Red Velvet'.");
    }

    @Test
    void atualizaEDesativa() {
        Cupcake existente = new Cupcake(3L, "Limão", new BigDecimal("10.00"), 1);
        when(repository.findById(3L)).thenReturn(Optional.of(existente));
        when(repository.existsBySaborIgnoreCaseAndIdNot("Limão Siciliano", 3L)).thenReturn(false);

        CupcakeResponse resp = service.atualizar(3L,
                new CupcakeRequest("Limão Siciliano", "Novo", new BigDecimal("11.50"), 7, null, false));

        assertThat(resp.sabor()).isEqualTo("Limão Siciliano");
        assertThat(resp.preco()).isEqualByComparingTo("11.50");
        assertThat(resp.ativo()).isFalse();
    }

    @Test
    void atualizarInexistenteGera404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.atualizar(9L, req("X"))).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void defineEstoque() {
        Cupcake c = new Cupcake(3L, "Limão", new BigDecimal("10.00"), 1);
        when(repository.findAllByIdForUpdate(List.of(3L))).thenReturn(List.of(c));
        assertThat(service.definirEstoque(3L, 40).estoque()).isEqualTo(40);
    }

    @Test
    void vitrineListaSomenteAtivos() {
        when(repository.findByAtivoTrueOrderBySaborAsc()).thenReturn(List.of(new Cupcake(1L, "A", BigDecimal.ONE, 1)));
        assertThat(service.listarVitrine()).hasSize(1);
    }
}
