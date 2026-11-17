# Plano de Testes de Verificação e Validação

**Verificação** ("construímos o produto certo?" do ponto de vista técnico): testes automatizados de unidade e
integração que conferem cada regra de negócio (RN) contra a especificação.
**Validação** ("construímos o produto que o cliente precisa?"): execução dos casos de uso ponta a ponta no
navegador, como o usuário faria, comparando comportamento **esperado × obtido**.

## 1. Estratégia e ambiente

| Nível | Ferramenta | Ambiente | Quantidade |
|-------|-----------|----------|------------|
| Unidade | JUnit 5 + Mockito + AssertJ | JVM, sem banco | 55 testes (6 classes) |
| Integração da API | `@SpringBootTest` + MockMvc | H2 modo PostgreSQL + migrations Flyway reais | 22 testes (5 classes) |
| Integração com SGBD de produção | Testcontainers / Postgres local | PostgreSQL 16 | 2 testes |
| Sistema / aceitação (E2E) | Playwright + Chromium headless | App em `localhost:8080` + PostgreSQL 16 | 21 verificações |
| Aceitação manual | Roteiro da seção 4 | Ambiente hospedado (Render) | 15 passos |

Execução de referência: 17/11/2026, macOS (Apple Silicon), JDK Temurin 21.0.13, PostgreSQL 16 (Homebrew),
Chromium headless 159.

Critério de aceite: 100% dos testes automatizados passando; nenhum erro de JavaScript no console; todos os
defeitos encontrados registrados no [laudo](laudo-de-qualidade.md) com evidência e correção (ou justificativa).

## 2. Testes automatizados (verificação)

Comando: `PG_TEST_URL=jdbc:postgresql://localhost:5432/cupcakes_test ./mvnw test`

| Classe | Alvo | Testes | Resultado |
|--------|------|-------:|-----------|
| `CalculadoraFreteTest` | RN02 (faixas 0; 11,90; 99,99; 100,00; 100,01; 350) | 7 | ✅ |
| `StatusPedidoTest` | RN07 (14 transições válidas/inválidas) | 14 | ✅ |
| `PixServiceTest` | RN09 (CRC16 de referência "123456789" = 29B1, campos EMV, QR PNG) | 5 | ✅ |
| `PedidoServiceTest` | RN01–RN05, RN07–RN10 com mocks | 21 | ✅ |
| `UsuarioServiceTest` | RN06 | 2 | ✅ |
| `CupcakeServiceTest` | CU08, RN11 | 6 | ✅ |
| `AuthApiIT` | CU04, MSG01–MSG03, CSRF, logout | 8 | ✅ |
| `PedidoApiIT` | CU01/05/06/07/10, MSG05, MSG08, MSG10 | 7 | ✅ |
| `SegurancaApiIT` | RN10, CU08, CU09, MSG06, MSG07 | 5 | ✅ |
| `AdminEstoqueIT` | RN11 / defeito D05 | 1 | ✅ (falhava antes da correção — E02) |
| `CheckoutConcorrenciaIT` | RN05 em H2 | 1 | ✅ |
| `CheckoutConcorrenciaPostgresIT` | RN05 e schema no PostgreSQL | 2 | ✅ (falha sem o lock — E01) |
| **Total** | | **79** | **79 ✅ · 0 falhas · 0 ignorados** |

Cobertura (JaCoCo): **95,1% das linhas**, **88,6% dos ramos**, 92,6% dos métodos.
Por camada: service 96,3% linhas / 90,1% ramos · controller 96,1% · config 97,3% · exception 90,4%.
Relatórios: [JaCoCo](evidencias/relatorios/jacoco/index.html) · [Surefire](evidencias/relatorios/surefire/surefire.html).

## 3. Casos de teste de sistema (validação) — esperado × obtido

Executados pelo roteiro automatizado [`scripts/e2e-capturas.mjs`](../../scripts/e2e-capturas.mjs) no navegador.
Saída completa: [antes das correções](evidencias/E04-e2e-antes-das-correcoes.txt) ·
[depois das correções](evidencias/E05-e2e-depois-das-correcoes.txt).

| ID | CU / RN | Procedimento | Resultado esperado | Resultado obtido | Status | Evidência |
|----|---------|-------------|--------------------|------------------|--------|-----------|
| CT01 | CU02 | Abrir a vitrine sem login | Lista de cupcakes visível | 8 cupcakes listados | ✅ | [T01](evidencias/T01-vitrine.png) |
| CT02 | CU02, RN01 | Observar "Coco Queimado" (estoque 0) | Selo "Esgotado", botão desabilitado | Conforme esperado | ✅ | [T01](evidencias/T01-vitrine.png) |
| CT03 | CU03 | Adicionar 3 Red Velvet e 2 Pistache | Contador "Carrinho (5)" | Contador = 5 | ✅ | — |
| CT04 | CU03, RN01 | Adicionar +9 Pistache (estoque 10, já há 2) | MSG04 "Só temos 10 unidade(s) de Pistache disponíveis." | Mensagem exibida, carrinho inalterado | ✅ | [T02](evidencias/T02-msg04-estoque-carrinho.png) |
| CT05 | CU03, RN02 | Abrir o carrinho | Subtotal R$ 70,50 + frete R$ 10,00 = R$ 80,50 | R$ 80,50 | ✅ | [T03](evidencias/T03-carrinho.png) |
| CT06 | CU04 | Finalizar sem login; senha errada | Redireciona ao login; MSG01 | Redirecionou; "E-mail ou senha inválidos." | ✅ | [T04](evidencias/T04-msg01-login-invalido.png) |
| CT07 | CU04, CU01 | Login correto | Volta ao checkout com endereço do cadastro | Endereço pré-preenchido | ✅ | [T05](evidencias/T05-checkout.png) |
| CT08 | CU01, CU05 | "Pagar com PIX" | Pedido AGUARDANDO_PAGAMENTO, QR Code + copia e cola iniciando em `000201` | 1ª execução: **FALHA** (defeito do roteiro, D01). Após correção: conforme esperado | ✅ | [T06](evidencias/T06-pix-resumo.png) |
| CT09 | CU01 | Após o pedido | Carrinho vazio | Contador = 0 | ✅ | [T06](evidencias/T06-pix-resumo.png) |
| CT10 | CU07, RN09 | "Simular pagamento PIX" | Status Pago, pagamento Confirmado, data do pagamento | Conforme esperado | ✅ | [T07](evidencias/T07-pagamento-confirmado.png) |
| CT11 | CU06 | Abrir "Meus pedidos" | Pedido listado com status Pago | Conforme esperado | ✅ | [T08](evidencias/T08-meus-pedidos.png) |
| CT12 | CU01-A1, RN05 | Item no carrinho; admin zera estoque; cliente paga | MSG05 "Estoque insuficiente para Limão Siciliano: disponível 0, solicitado 1."; nenhum pedido criado | Conforme esperado | ✅ | [T09](evidencias/T09-msg05-estoque-checkout.png) |
| CT13 | CU08 | Admin salva cupcake com sabor vazio, preço 0, estoque −1 | MSG03 com erro em cada campo | 3 campos destacados | ✅ | [T10](evidencias/T10-admin-catalogo-validacao.png) |
| CT14 | CU08 | Admin cadastra "Morango com Nutella" | Aparece na tabela | Conforme esperado | ✅ | — |
| CT15 | CU09, RN07 | Admin avança pedido Pago → Em preparo | Status "Em preparo" | Conforme esperado | ✅ | [T11](evidencias/T11-admin-pedidos.png) |
| CT16 | RN10 | Cliente abre `admin.html` | MSG07 "Acesso restrito ao administrador." | Conforme esperado | ✅ | — |
| CT17 | CU04, RN06 | Cadastro com senha "123" | Erro no campo senha (MSG03) | "A senha deve ter entre 6 e 72 caracteres." | ✅ | — |
| CT18 | CU04, RN06 | Cadastro com e-mail já existente | MSG02 | Conforme esperado | ✅ | [T12](evidencias/T12-msg02-cadastro-duplicado.png) |
| CT19 | IHC | Vitrine em 390 px (celular) | Sem rolagem horizontal, cards em coluna | Conforme esperado | ✅ | [T13](evidencias/T13-vitrine-celular.png) |
| CT20 | — | Console do navegador em todo o roteiro | Nenhum erro de JavaScript | Nenhum erro | ✅ | E05 |
| CT21 | CU03, RN02 | Item no carrinho; admin muda preço Red Velvet para 13,90; cliente abre carrinho | Linha mostra R$ 13,90 e soma das linhas = subtotal | 1ª execução: **FALHA** — linha R$ 12,90, subtotal R$ 25,40 (defeito D04). Após correção: conforme esperado | ✅ | [antes](evidencias/E03-ANTES-carrinho-preco-desatualizado.png) · [depois](evidencias/T14-carrinho-preco-atualizado.png) |

## 4. Roteiro de teste manual (ambiente hospedado)

> Executado pelo João no link do Render, com captura de tela de cada passo salva em `evidencias/manual/`.
> Preencher a coluna **Obtido** e **OK?**. Usar uma aba anônima para o cliente e outra janela para o admin.

| # | Passo | Esperado | Obtido | OK? |
|---|-------|----------|--------|-----|
| M01 | Abrir o link hospedado (pode levar ~1 min no 1º acesso) | Vitrine com 8 cupcakes; "Coco Queimado" esgotado | | |
| M02 | Abrir no celular | Layout em uma coluna, sem rolagem lateral | | |
| M03 | Adicionar 2 sabores ao carrinho | Contador atualiza | | |
| M04 | Tentar adicionar mais que o estoque | MSG04 | | |
| M05 | Abrir carrinho, alterar quantidade, remover item | Totais recalculados; frete grátis ao passar de R$ 100 | | |
| M06 | Finalizar → login com senha errada | MSG01 | | |
| M07 | Criar conta nova | Logado e de volta ao checkout | | |
| M08 | Pagar com PIX | QR Code e código copia e cola; "Copiar código" funciona | | |
| M09 | Simular pagamento | Status Pago | | |
| M10 | Novo pedido e "Cancelar pedido" | Status Cancelado; estoque volta (conferir na vitrine) | | |
| M11 | Meus pedidos | Os dois pedidos com status corretos | | |
| M12 | Admin: login, cadastrar cupcake, desativar outro | Novo aparece na vitrine; desativado some | | |
| M13 | Admin: zerar estoque de um item que está no carrinho do cliente; cliente paga | MSG05, nenhum pedido criado | | |
| M14 | Admin: avançar pedido até Entregue | Botões seguem a ordem; status final Entregue | | |
| M15 | Cliente tenta abrir `/admin.html` | MSG07 | | |
