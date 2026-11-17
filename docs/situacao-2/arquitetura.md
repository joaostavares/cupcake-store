# Arquitetura

## Visão geral

```
Navegador (HTML/CSS/JS puro)  ──fetch JSON + cookie de sessão──▶  Spring Boot (uma aplicação, um deploy)
   static/*.html  (View)                                           controller  →  service  →  repository  →  PostgreSQL
                                                                     (C)          (regras)      (JPA)          (Flyway)
```

O padrão **MVC** está dividido assim:

| Camada | Pacote | Responsabilidade |
|--------|--------|------------------|
| **Model** | `model` | Entidades JPA (`Usuario`, `Cupcake`, `Pedido`, `ItemPedido`, `Pagamento`) e enums. Sem regra de negócio — só estado e associações. |
| **View** | `src/main/resources/static` | Páginas HTML + CSS + JavaScript (módulos ES, sem framework). Consomem a API REST. |
| **Controller** | `controller` | Endpoints REST, validação de entrada (Bean Validation nos DTOs) e tradução HTTP. Não contém regra de negócio. |
| Service | `service` | Regras de negócio e transações: `PedidoService` (RN01–RN05, RN07–RN10), `CalculadoraFrete` (RN02), `PixService` (RN09), `CupcakeService`, `UsuarioService` (RN06). |
| Repository | `repository` | Spring Data JPA. `CupcakeRepository.findAllByIdForUpdate` faz `SELECT ... FOR UPDATE`. |
| DTO | `dto` | Records de entrada/saída — a entidade nunca sai pela API (evita vazar `senhaHash` e laços de serialização). |
| Exceções | `exception` | `NegocioException` e filhas + `GlobalExceptionHandler` (`@RestControllerAdvice`) que gera **ProblemDetail (RFC 9457)**. |
| Segurança | `config`, `security` | Spring Security com sessão, BCrypt, CSRF, papéis. |

Diagrama: [../situacao-1/uml/classes-camadas.png](../situacao-1/uml/classes-camadas.png).

## API REST

| Método | Caminho | Acesso | Descrição | Respostas |
|--------|---------|--------|-----------|-----------|
| GET | `/api/cupcakes` | público | Vitrine (só ativos) | 200 |
| GET | `/api/cupcakes/{id}` | público | Detalhe | 200, 404 |
| POST | `/api/auth/cadastro` | público | Cadastro + login automático | 201, 400, 409 |
| POST | `/api/auth/login` | público | Login | 200, 400, 401 |
| POST | `/api/auth/logout` | logado | Logout | 204 |
| GET | `/api/auth/me` | público | Usuário da sessão | 200, 204 |
| POST | `/api/pedidos/cotacao` | público | Subtotal/frete/total do carrinho | 200, 400, 404, 409 |
| POST | `/api/pedidos` | cliente | **Finalizar compra** | **201** + `Location`, 400, 401, 404, 409 |
| GET | `/api/pedidos` | cliente | Meus pedidos | 200 |
| GET | `/api/pedidos/{id}` | dono/admin | Detalhe | 200, 404 |
| GET | `/api/pedidos/{id}/pagamento/qrcode` | dono/admin | QR Code PNG | 200, 404 |
| POST | `/api/pedidos/{id}/pagamento/confirmar` | dono/admin | Simula confirmação do PSP | 200, 404, 422 |
| POST | `/api/pedidos/{id}/cancelar` | dono/admin | Cancela e devolve estoque | 200, 404, 422 |
| GET | `/api/admin/cupcakes` | admin | Todos os cupcakes | 200, 403 |
| POST | `/api/admin/cupcakes` | admin | Cadastrar | 201, 400, 409 |
| PUT | `/api/admin/cupcakes/{id}` | admin | Editar / ativar / desativar | 200, 400, 404, 409 |
| PATCH | `/api/admin/cupcakes/{id}/estoque` | admin | Definir estoque | 200, 400, 404 |
| GET | `/api/admin/pedidos?status=` | admin | Pedidos (filtro opcional) | 200 |
| PATCH | `/api/admin/pedidos/{id}/status` | admin | Avançar status (RN07) | 200, 400, 404, 422 |
| GET | `/actuator/health` | público | Health check do deploy | 200 |

Exemplo de erro (MSG05):

```json
{
  "type": "about:blank",
  "title": "Estoque insuficiente",
  "status": 409,
  "detail": "Estoque insuficiente para Pistache: disponível 1, solicitado 2.",
  "instance": "/api/pedidos",
  "cupcakeId": 5, "disponivel": 1, "solicitado": 2
}
```

## Decisões técnicas

1. **Checkout transacional com bloqueio pessimista** (`PESSIMISTIC_WRITE`, ids ordenados para evitar deadlock).
   Escolhido em vez de lock otimista porque o conflito é provável justamente nos itens mais populares e o
   cliente não deveria ver "tente de novo" por uma disputa que o banco resolve esperando milissegundos.
   Provado pelo teste `CheckoutConcorrenciaPostgresIT` (10 compras simultâneas, 5 unidades).
2. **Sessão + cookie, não JWT.** Front e API estão na mesma origem; sessão é mais simples, revogável no logout
   e o cookie é `HttpOnly`. CSRF fica ativo com o padrão *double submit cookie* (`XSRF-TOKEN`).
3. **Erros como ProblemDetail.** Formato padronizado (RFC 9457) consumido por um único tratador no front (`api.js`).
4. **Pedidos de outros clientes retornam 404**, não 403, para não revelar que o id existe.
5. **Flyway + `ddl-auto=validate`.** O schema é definido só pelas migrations; o Hibernate apenas confere.
6. **Front sem framework**, servido pelo próprio Spring Boot: um único artefato e um único deploy, como pedido
   pela disciplina (HTML/CSS/JS). Módulos ES nativos organizam o código.
7. **Content-Security-Policy restritiva** (`script-src 'self'`, sem inline): reduz impacto de XSS. Todo texto vindo
   da API é escapado (`esc()`) antes de ir para o HTML.
8. **Admin e cliente de demonstração criados na inicialização** (`DadosIniciais`), com senha vinda de variável de
   ambiente — a senha de produção não fica no repositório.
9. **H2 em modo PostgreSQL nos testes** (as mesmas migrations rodam nele) para a suíte rodar em qualquer máquina;
   os testes de PostgreSQL real rodam com Testcontainers (Docker) ou apontando para um Postgres existente.

## Testes

| Tipo | Ferramenta | Classes |
|------|-----------|---------|
| Unitário | JUnit 5 + Mockito + AssertJ | `PedidoServiceTest`, `CalculadoraFreteTest`, `StatusPedidoTest`, `PixServiceTest`, `UsuarioServiceTest`, `CupcakeServiceTest` |
| Integração (API) | `@SpringBootTest` + MockMvc + H2/Flyway | `AuthApiIT`, `PedidoApiIT`, `SegurancaApiIT`, `CheckoutConcorrenciaIT` |
| Integração (PostgreSQL) | Testcontainers / Postgres local | `CheckoutConcorrenciaPostgresIT` |
| Ponta a ponta (navegador) | Playwright (Chromium) | `scripts/e2e-capturas.mjs` |
| Cobertura | JaCoCo | `target/site/jacoco/index.html` |
