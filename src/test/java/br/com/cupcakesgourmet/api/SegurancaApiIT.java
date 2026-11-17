package br.com.cupcakesgourmet.api;

import br.com.cupcakesgourmet.model.Cupcake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Segurança e administração — RN10, CU08, CU09")
class SegurancaApiIT extends ApiTestSupport {

    @Test
    @DisplayName("vitrine e front estático são públicos (CU02)")
    void vitrinePublica() throws Exception {
        mvc.perform(get("/api/cupcakes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sabor", hasItem("Red Velvet")));
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("checkout sem login retorna 401 — MSG06")
    void checkoutExigeLogin() throws Exception {
        postJson("/api/pedidos", pedido(1, 1), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Faça login para continuar."));
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cliente não acessa a área administrativa (403) — MSG07")
    void clienteNaoEhAdmin() throws Exception {
        MockHttpSession cliente = novoCliente();
        mvc.perform(get("/api/admin/cupcakes").session(cliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Acesso restrito ao administrador."));
        mvc.perform(get("/api/admin/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin cadastra, edita, desativa e ajusta estoque de cupcake (CU08)")
    void adminGerenciaCatalogo() throws Exception {
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_SENHA);
        String sabor = "Admin " + UUID.randomUUID().toString().substring(0, 6);

        long id = corpo(postJson("/api/admin/cupcakes", """
                {"sabor":"%s","descricao":"Novo","preco":9.90,"estoque":5}""".formatted(sabor), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))).get("id").asLong();

        postJson("/api/admin/cupcakes", """
                {"sabor":"%s","preco":9.90,"estoque":5}""".formatted(sabor.toUpperCase()), admin)
                .andExpect(status().isConflict());

        postJson("/api/admin/cupcakes", """
                {"sabor":"X","preco":0,"estoque":-1}""", admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.preco").value("O preço deve ser maior que zero."))
                .andExpect(jsonPath("$.erros.estoque").value("O estoque não pode ser negativo."));

        mvc.perform(patch("/api/admin/cupcakes/{id}/estoque", id).with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estoque\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estoque").value(50));

        mvc.perform(put("/api/admin/cupcakes/{id}", id).with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sabor":"%s","preco":10.50,"estoque":50,"ativo":false}""".formatted(sabor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(get("/api/cupcakes")).andExpect(jsonPath("$[*].sabor", not(hasItem(sabor))));
        mvc.perform(get("/api/cupcakes/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/cupcakes").session(admin)).andExpect(jsonPath("$[*].sabor", hasItem(sabor)));
    }

    @Test
    @DisplayName("admin movimenta pedido pela máquina de estados (CU09, RN07)")
    void adminAtualizaStatus() throws Exception {
        Cupcake c = novoCupcake("50.00", 5);
        long id = corpo(postJson("/api/pedidos", pedido(c.getId(), 2), novoCliente())).get("id").asLong();
        MockHttpSession admin = login(ADMIN_EMAIL, ADMIN_SENHA);

        alterarStatus(admin, id, "EM_PREPARO").andExpect(status().isUnprocessableEntity());
        alterarStatus(admin, id, "PAGO").andExpect(jsonPath("$.status").value("PAGO"));
        alterarStatus(admin, id, "EM_PREPARO").andExpect(jsonPath("$.status").value("EM_PREPARO"));
        alterarStatus(admin, id, "ENVIADO").andExpect(jsonPath("$.status").value("ENVIADO"));
        alterarStatus(admin, id, "ENTREGUE").andExpect(jsonPath("$.status").value("ENTREGUE"));
        alterarStatus(admin, id, "CANCELADO").andExpect(status().isUnprocessableEntity());
        alterarStatus(admin, id, "INEXISTENTE").andExpect(status().isBadRequest());

        mvc.perform(get("/api/admin/pedidos").param("status", "ENTREGUE").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }

    private org.springframework.test.web.servlet.ResultActions alterarStatus(MockHttpSession admin, long id, String status)
            throws Exception {
        return mvc.perform(patch("/api/admin/pedidos/{id}/status", id).with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }
}
