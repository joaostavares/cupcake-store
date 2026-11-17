package br.com.cupcakesgourmet.api;

import br.com.cupcakesgourmet.model.Cupcake;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API /api/pedidos — CU01, CU05, CU06, CU07, CU10")
class PedidoApiIT extends ApiTestSupport {

    @Test
    @DisplayName("fluxo completo: cotação → pedido 201 → PIX → confirmação → meus pedidos")
    void fluxoCompleto() throws Exception {
        Cupcake c = novoCupcake("12.90", 10);

        postJson("/api/pedidos/cotacao", """
                {"itens":[{"cupcakeId":%d,"quantidade":3}]}""".formatted(c.getId()), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(38.70))
                .andExpect(jsonPath("$.frete").value(10.00))
                .andExpect(jsonPath("$.total").value(48.70));

        MockHttpSession sessao = novoCliente();
        JsonNode pedido = corpo(postJson("/api/pedidos", pedido(c.getId(), 3), sessao)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/pedidos/")))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.total").value(48.70))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(12.90))
                .andExpect(jsonPath("$.pagamento.status").value("PENDENTE"))
                .andExpect(jsonPath("$.pagamento.codigoPix", startsWith("000201"))));
        long id = pedido.get("id").asLong();

        assertThat(cupcakeRepository.findById(c.getId()).orElseThrow().getEstoque()).isEqualTo(7);

        mvc.perform(get("/api/pedidos/{id}/pagamento/qrcode", id).session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        postJson("/api/pedidos/" + id + "/pagamento/confirmar", "", sessao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.pagamento.status").value("CONFIRMADO"));

        postJson("/api/pedidos/" + id + "/pagamento/confirmar", "", sessao)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Não é possível mudar o pedido de PAGO para PAGO."));

        mvc.perform(get("/api/pedidos").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    @DisplayName("estoque insuficiente no checkout retorna 409 e não altera o estoque — MSG05")
    void estoqueInsuficiente() throws Exception {
        Cupcake c = novoCupcake("10.00", 2);
        postJson("/api/pedidos", pedido(c.getId(), 3), novoCliente())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", endsWith("disponível 2, solicitado 3.")))
                .andExpect(jsonPath("$.disponivel").value(2));
        assertThat(cupcakeRepository.findById(c.getId()).orElseThrow().getEstoque()).isEqualTo(2);
    }

    @Test
    @DisplayName("cancelamento pelo cliente devolve o estoque (RN08)")
    void cancelamentoDevolveEstoque() throws Exception {
        Cupcake c = novoCupcake("10.00", 5);
        MockHttpSession sessao = novoCliente();
        long id = corpo(postJson("/api/pedidos", pedido(c.getId(), 4), sessao)).get("id").asLong();
        assertThat(cupcakeRepository.findById(c.getId()).orElseThrow().getEstoque()).isEqualTo(1);

        postJson("/api/pedidos/" + id + "/cancelar", "", sessao)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.pagamento.status").value("CANCELADO"));
        assertThat(cupcakeRepository.findById(c.getId()).orElseThrow().getEstoque()).isEqualTo(5);
    }

    @Test
    @DisplayName("carrinho vazio e quantidade acima de 20 retornam 400")
    void validacoesDoPedido() throws Exception {
        MockHttpSession sessao = novoCliente();
        postJson("/api/pedidos", """
                {"itens":[],"enderecoEntrega":"Rua X"}""", sessao)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Seu carrinho está vazio."));
        postJson("/api/pedidos", pedido(1, 21), sessao)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros['itens[0].quantidade']").value("A quantidade máxima por item é 20."));
        postJson("/api/pedidos", """
                {"itens":[{"cupcakeId":1,"quantidade":1}],"enderecoEntrega":" "}""", sessao)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.enderecoEntrega").value("Informe o endereço de entrega."));
        postJson("/api/pedidos", "{not json", sessao)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O corpo da requisição está mal formatado."));
    }

    @Test
    @DisplayName("cupcake esgotado do seed (Coco Queimado) não pode ser comprado")
    void cupcakeEsgotadoDoSeed() throws Exception {
        long coco = cupcakeRepository.findAll().stream().filter(c -> c.getSabor().equals("Coco Queimado"))
                .findFirst().orElseThrow().getId();
        postJson("/api/pedidos", pedido(coco, 1), novoCliente()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("cliente não vê nem altera pedido de outro cliente (404) — RN10")
    void isolamentoEntreClientes() throws Exception {
        Cupcake c = novoCupcake("10.00", 5);
        long id = corpo(postJson("/api/pedidos", pedido(c.getId(), 1), novoCliente())).get("id").asLong();
        MockHttpSession intruso = novoCliente();

        mvc.perform(get("/api/pedidos/{id}", id).session(intruso))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pedido não encontrado."));
        postJson("/api/pedidos/" + id + "/cancelar", "", intruso).andExpect(status().isNotFound());
        postJson("/api/pedidos/" + id + "/pagamento/confirmar", "", intruso).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("id não numérico retorna 400")
    void idInvalido() throws Exception {
        mvc.perform(get("/api/pedidos/abc").session(novoCliente()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Valor inválido para 'id'."));
    }
}
