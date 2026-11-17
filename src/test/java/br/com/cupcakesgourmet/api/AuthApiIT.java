package br.com.cupcakesgourmet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API /api/auth — CU04 cadastro e login")
class AuthApiIT extends ApiTestSupport {

    @Test
    @DisplayName("cadastro cria cliente, já autentica e /me retorna os dados")
    void cadastroAutentica() throws Exception {
        MockHttpSession sessao = novoCliente();
        mvc.perform(get("/api/auth/me").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("CLIENTE"))
                .andExpect(jsonPath("$.endereco").value("Rua Teste, 1"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    @DisplayName("e-mail duplicado (ignorando maiúsculas) retorna 409 — MSG02")
    void emailDuplicado() throws Exception {
        postJson("/api/auth/cadastro", """
                {"nome":"X","email":"CLIENTE@cupcakes.com","senha":"segredo1"}""", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este e-mail já está cadastrado. Faça login ou use outro e-mail."));
    }

    @Test
    @DisplayName("campos inválidos retornam 400 com erro por campo — MSG03")
    void validacao() throws Exception {
        postJson("/api/auth/cadastro", """
                {"nome":"","email":"invalido","senha":"123"}""", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Verifique os campos destacados."))
                .andExpect(jsonPath("$.erros.nome").value("Informe seu nome."))
                .andExpect(jsonPath("$.erros.email").value("E-mail inválido."))
                .andExpect(jsonPath("$.erros.senha").value("A senha deve ter entre 6 e 72 caracteres."));
    }

    @Test
    @DisplayName("login com senha errada retorna 401 — MSG01")
    void loginInvalido() throws Exception {
        postJson("/api/auth/login", new Login("cliente@cupcakes.com", "errada"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        postJson("/api/auth/login", new Login("naoexiste-" + UUID.randomUUID() + "@x.com", "qualquer"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("login do cliente demo funciona e e-mail não diferencia maiúsculas")
    void loginValido() throws Exception {
        postJson("/api/auth/login", new Login("Cliente@Cupcakes.com", "Cliente@123"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@cupcakes.com"));
    }

    @Test
    @DisplayName("visitante recebe 204 em /me")
    void meVisitante() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST sem token CSRF é bloqueado (403)")
    void semCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente@cupcakes.com\",\"senha\":\"Cliente@123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sessão expirada"));
    }

    @Test
    @DisplayName("logout encerra a sessão")
    void logout() throws Exception {
        MockHttpSession sessao = novoCliente();
        postJson("/api/auth/logout", "", sessao).andExpect(status().isNoContent());
        mvc.perform(get("/api/pedidos").session(sessao)).andExpect(status().isUnauthorized());
    }
}
