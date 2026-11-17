package br.com.cupcakesgourmet.api;

import br.com.cupcakesgourmet.model.Cupcake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Defeito D05 (laudo de qualidade): editar o cupcake não pode desfazer vendas feitas enquanto o formulário estava aberto. */
@DisplayName("D05 — edição do cupcake não sobrescreve o estoque (lost update)")
class AdminEstoqueIT extends ApiTestSupport {

    @Test
    void edicaoNaoDesfazVendaConcorrente() throws Exception {
        Cupcake c = novoCupcake("10.00", 10);
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        // 1. Admin abre o formulário de edição: vê estoque 10.
        // 2. Enquanto isso, um cliente compra 3 unidades -> estoque 7.
        postJson("/api/pedidos", pedido(c.getId(), 3), novoCliente()).andExpect(status().isCreated());
        // 3. Admin salva só a mudança de preço, mas o formulário ainda carrega "estoque": 10.
        mvc.perform(put("/api/admin/cupcakes/{id}", c.getId()).with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sabor":"%s","preco":11.00,"estoque":10,"ativo":true}""".formatted(c.getSabor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(11.00));

        assertThat(cupcakeRepository.findById(c.getId()).orElseThrow().getEstoque())
                .as("as 3 unidades vendidas não podem voltar ao estoque")
                .isEqualTo(7);
    }
}
