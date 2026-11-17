package br.com.cupcakesgourmet.api;

import br.com.cupcakesgourmet.model.Cupcake;
import br.com.cupcakesgourmet.repository.CupcakeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base dos testes de API: contexto Spring completo, H2 em modo PostgreSQL com as mesmas migrations Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestSupport {

    static final String ADMIN_EMAIL = "admin@cupcakes.com";
    static final String ADMIN_SENHA = "Admin@123";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    CupcakeRepository cupcakeRepository;

    /** Cada teste cria seus próprios cupcakes para não depender do seed nem de outros testes. */
    Cupcake novoCupcake(String preco, int estoque) {
        Cupcake c = new Cupcake();
        c.setSabor("Teste " + UUID.randomUUID().toString().substring(0, 8));
        c.setPreco(new BigDecimal(preco));
        c.setEstoque(estoque);
        return cupcakeRepository.save(c);
    }

    MockHttpSession login(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login(email, senha))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    MockHttpSession novoCliente() throws Exception {
        String email = "cli-" + UUID.randomUUID() + "@teste.com";
        MvcResult r = mvc.perform(post("/api/auth/cadastro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Cliente Teste","email":"%s","senha":"segredo1","endereco":"Rua Teste, 1"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    ResultActions postJson(String url, Object body, MockHttpSession session) throws Exception {
        var req = post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body instanceof String s ? s : json.writeValueAsString(body));
        if (session != null) {
            req.session(session);
        }
        return mvc.perform(req);
    }

    JsonNode corpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    static String pedido(long cupcakeId, int qtd) {
        return """
                {"itens":[{"cupcakeId":%d,"quantidade":%d}],"enderecoEntrega":"Rua das Flores, 123"}
                """.formatted(cupcakeId, qtd);
    }

    record Login(String email, String senha) {
    }
}
