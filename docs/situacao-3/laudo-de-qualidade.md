# Laudo de Qualidade

**Sistema:** Cupcakes Gourmet · **Versão avaliada:** commit `91b86ff` (primeira versão completa) · **Versão corrigida:** commit `554491d`
**Data:** 17/11/2026 · **Responsável:** João Serafini Tavares

## 1. Resumo

| Indicador | Resultado |
|-----------|-----------|
| Testes automatizados | **79 executados, 79 aprovados**, 0 ignorados (unidade, integração H2 e PostgreSQL) |
| Cobertura de código (JaCoCo) | **95,1% das linhas · 88,6% dos ramos** |
| Casos de teste de sistema (navegador) | **21/21 aprovados** após correções (19/20 e 20/21 nas execuções anteriores) |
| Erros de JavaScript no console | 0 |
| Defeitos encontrados | **6** (1 alto, 1 médio, 3 baixos, 1 preventivo) + 1 falha de concorrência evitada no projeto |
| Defeitos corrigidos | **6 de 6** |
| Limitações conhecidas (não são defeitos) | 6 — seção 4 |

**Parecer:** o sistema atende aos casos de uso CU01–CU10 e às regras RN01–RN11. Todos os defeitos encontrados na
validação foram corrigidos, cobertos por teste automatizado e documentados abaixo com evidência.

## 2. Como os defeitos foram encontrados

1. **Testes automatizados** escritos junto com cada camada (JUnit/Mockito/MockMvc).
2. **Teste de concorrência com prova negativa**: o teste foi executado *sem* o mecanismo de proteção para
   comprovar que ele realmente detecta o problema.
3. **Roteiro E2E no navegador** (`scripts/e2e-capturas.mjs`) cobrindo todos os casos de uso, com capturas de tela.
4. **Inspeção visual** das capturas de tela (desktop e celular).
5. **Revisão de código** focada em concorrência e consistência de dados.

## 3. Defeitos encontrados e correções

### D01 — Roteiro de teste verificava o QR Code antes de a imagem carregar
| | |
|---|---|
| **Severidade** | Baixa (defeito no *teste*, não no sistema) |
| **Como foi encontrado** | 1ª execução do roteiro E2E: CT08 reprovado |
| **Esperado × obtido** | Esperado: CT08 aprovado (QR Code visível). Obtido: `FALHA CT08`, embora a captura T06 mostrasse o QR Code corretamente |
| **Causa** | O roteiro lia `img.complete` logo após a página abrir; o PNG do QR Code ainda estava sendo baixado (condição de corrida) |
| **Correção** | O roteiro passou a *aguardar* o carregamento (`waitForFunction` com timeout de 5 s) |
| **Evidência** | 1ª execução, 19/20: [E00](evidencias/E00-e2e-primeira-execucao.txt). 2ª execução, CT08 OK: [E04](evidencias/E04-e2e-antes-das-correcoes.txt) |
| **Lição** | Falso negativo em teste também é defeito: um teste instável gera desconfiança na suíte inteira |

### D02 — Imagem do cupcake cortada na vitrine
| | |
|---|---|
| **Severidade** | Baixa (visual) |
| **Como foi encontrado** | Inspeção da captura T01 |
| **Esperado × obtido** | Esperado: ilustração inteira. Obtido: topo da cereja cortado |
| **Causa** | `object-fit: cover` num contêiner 4:3 com imagem quadrada |
| **Correção** | `object-fit: contain` em `style.css` |
| **Evidência** | [T01 atual](evidencias/T01-vitrine.png) |

### D03 — Espaçamentos e controles sem estilo
| | |
|---|---|
| **Severidade** | Baixa (visual / consistência) |
| **Como foi encontrado** | Inspeção das capturas T03, T09 e T11 |
| **Esperado × obtido** | Esperado: títulos alinhados ao topo dos painéis e controles com o mesmo estilo. Obtido: margem extra acima de "Resumo"/"Itens"; "Forma de pagamento" com a moldura padrão do navegador; filtro de status pequeno e sem estilo |
| **Correção** | Regras CSS para `.painel > h2:first-child`, `fieldset.campo` e `.campo-inline select` |
| **Evidência** | Antes: [T09 de antes](evidencias/E03-ANTES-carrinho-preco-desatualizado.png) (margem acima de "Resumo"). Depois: [T03](evidencias/T03-carrinho.png), [T05](evidencias/T05-checkout.png) |

### D04 — Carrinho mostrava preço antigo após alteração de preço
| | |
|---|---|
| **Severidade** | **Média** (informação financeira inconsistente na tela; o valor cobrado sempre foi o correto) |
| **Como foi encontrado** | Caso de teste CT21 (cenário criado na revisão: preço muda com item já no carrinho) |
| **Esperado × obtido** | Esperado: linha Red Velvet R$ 13,90 e soma das linhas = subtotal. Obtido: linha R$ 12,90 (preço salvo no navegador) e subtotal R$ 25,40 (preço atual do servidor) — as linhas somavam R$ 24,40 |
| **Causa** | As linhas usavam o preço guardado no `localStorage` no momento em que o item foi adicionado; só o resumo vinha do servidor |
| **Correção** | `carrinho.sincronizar()` atualiza preço, sabor e estoque guardados com os dados da cotação do servidor a cada exibição do carrinho |
| **Evidência** | Antes: [E03](evidencias/E03-ANTES-carrinho-preco-desatualizado.png) e [E04](evidencias/E04-e2e-antes-das-correcoes.txt) (`FALHA CT21`). Depois: [T14](evidencias/T14-carrinho-preco-atualizado.png) e [E05](evidencias/E05-e2e-depois-das-correcoes.txt) (21/21) |

### D05 — Editar o cupcake desfazia vendas (lost update de estoque)
| | |
|---|---|
| **Severidade** | **Alta** (permite vender acima do estoque físico) |
| **Como foi encontrado** | Revisão de código do `CupcakeService.atualizar`, confirmada por teste automatizado |
| **Esperado × obtido** | Cenário: estoque 10 → admin abre a edição → cliente compra 3 (estoque 7) → admin salva só uma mudança de preço. Esperado: estoque 7. Obtido: **estoque 10** — as 3 unidades vendidas voltaram para a vitrine |
| **Causa** | O formulário de edição enviava o estoque que estava na tela, e o `PUT` gravava esse valor desatualizado por cima do estoque real |
| **Correção** | Nova regra **RN11**: a edição cadastral não altera o estoque; ele só muda por venda, cancelamento ou pela operação específica "definir estoque" (`PATCH`, com bloqueio de linha). No admin, o campo Estoque fica bloqueado na edição, com orientação ao usuário |
| **Teste** | `AdminEstoqueIT` (falhava antes; passa depois) + asserção em `CupcakeServiceTest` |
| **Evidência** | Antes: [E02](evidencias/E02-lost-update-estoque.txt) (`expected: 7 but was: 10`). Depois: relatório [Surefire](evidencias/relatorios/surefire/surefire.html) (79/79) |

### D06 — Build Docker dependia de ferramentas não garantidas na imagem
| | |
|---|---|
| **Severidade** | Preventiva (risco de falha no deploy) |
| **Como foi encontrado** | Revisão do `Dockerfile` antes do deploy |
| **Causa** | O `mvnw` baixa o Maven com `curl`/`wget`/`unzip`, que podem não existir na imagem base |
| **Correção** | Estágio de build passou a usar a imagem oficial `maven:3.9.11-eclipse-temurin-21` |

### Falha de concorrência evitada no projeto (registro)
O problema 10 da modelagem do PIT I (estoque validado só no carrinho) foi tratado com bloqueio pessimista desde
a primeira versão. Para provar que a proteção é necessária e que o teste a detecta, o teste
`CheckoutConcorrenciaPostgresIT` foi executado **sem** o `@Lock`: **10 pedidos foram aceitos para 5 unidades**.
Com o lock: 5 aceitos, 5 recusados com MSG05, estoque final 0. Evidência: [E01](evidencias/E01-concorrencia-sem-lock.txt).

## 4. Limitações conhecidas (fora do escopo, não são defeitos)

| # | Limitação | Mitigação / trabalho futuro |
|---|-----------|------------------------------|
| L1 | PIX simulado: chave fictícia, sem PSP | Integrar um PSP (Mercado Pago, Efí) com webhook assinado no lugar do botão "Simular pagamento" |
| L2 | PIX não expira automaticamente | Job agendado cancelando pedidos `AGUARDANDO_PAGAMENTO` após 30 min (devolvendo estoque) |
| L3 | Sem limite de tentativas de login | Rate limiting (ex.: Bucket4j) e bloqueio temporário |
| L4 | Sem recuperação de senha | Fluxo com token por e-mail |
| L5 | "Definir estoque" do admin grava o valor absoluto informado | Comportamento intencional (contagem física manda); alternativa: operação de "entrada" (+N) |
| L6 | Plano gratuito do Render: hibernação após 15 min e PostgreSQL gratuito expira em 30 dias | Plano pago ou Postgres externo (Neon), já suportado via `DB_*`/`DB_PARAMS` |

## 5. Evidências

Todas em [`evidencias/`](evidencias/):

| Arquivo | Conteúdo |
|---------|----------|
| `E00-e2e-primeira-execucao.txt` | Roteiro E2E: 19/20 (D01) |
| `E01-concorrencia-sem-lock.txt` | Prova negativa do teste de concorrência |
| `E02-lost-update-estoque.txt` | D05 reproduzido antes da correção |
| `E03-ANTES-carrinho-preco-desatualizado.png` | D04 antes da correção |
| `E04-e2e-antes-das-correcoes.txt` | Roteiro E2E: 20/21 (CT21 falhando) |
| `E05-e2e-depois-das-correcoes.txt` | Roteiro E2E: 21/21 |
| `T01`–`T14` `.png` | Capturas de cada caso de teste após as correções |
| `relatorios/jacoco/index.html` | Cobertura de código |
| `relatorios/surefire/surefire.html` | Relatório dos 79 testes |
