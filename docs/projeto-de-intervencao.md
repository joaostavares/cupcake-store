# Projeto de Intervenção — rascunho

> Rascunho gerado a partir do que foi construído. **Revisar o texto** e preencher os campos marcados com ✏️.

**Aluno:** João Serafini Tavares
**Disciplina:** Projeto Integrador Transdisciplinar em Engenharia de Software II
**Tempo dedicado:** ✏️ _preencher_

## Objetivo

Implementar, testar, documentar e publicar o sistema **Cupcakes Gourmet** planejado no PIT I: uma loja web na
qual o cliente consulta o catálogo de cupcakes, monta um carrinho, finaliza o pedido e paga via PIX, e na qual o
administrador mantém o catálogo, o estoque e o andamento dos pedidos.

## Revisão de conceito

A revisão do planejamento do PIT I identificou 14 problemas na modelagem, entre eles: regras de negócio citadas
mas não definidas (RN01 e RN02), ausência de e-mail e senha no cliente apesar de existir login, valores monetários
em `Double`, status como texto livre, ausência das entidades de pagamento e de administrador, preço do item não
registrado no pedido e validação de estoque feita apenas no carrinho — o que permitiria vender mais do que o
estoque quando dois clientes compram ao mesmo tempo. Todos foram corrigidos: as regras foram escritas (RN01–RN11),
os diagramas UML foram refeitos em PlantUML, o modelo de dados passou pelos níveis conceitual, lógico (3FN) e
físico em PostgreSQL com dicionário de dados, e o "aplicativo mobile" virou uma aplicação web responsiva, como
pede a entrega em HTML, CSS e JavaScript.

## Metodologia

- **Arquitetura MVC em camadas** (controller → service → repository → model) em Java 21 com Spring Boot 3.5;
  front-end em HTML, CSS e JavaScript sem framework, servido pela própria aplicação.
- **Banco PostgreSQL** com schema versionado por migrations Flyway (projeto físico).
- **Desenvolvimento incremental** com commits por etapa no GitHub: planejamento → back-end → testes → front-end
  → validação → correções.
- **Testes em vários níveis**: unidade (JUnit 5 + Mockito), integração da API (MockMvc), integração com o
  PostgreSQL real (Testcontainers), sistema no navegador (Playwright) e cobertura (JaCoCo).
- **Verificação e validação** com casos de teste "esperado × obtido", prova negativa do teste de concorrência e
  inspeção das telas.

## Procedimento

1. Revisão do escopo, dos casos de uso, dos diagramas e da IHC do PIT I; criação de wireframes e catálogo de
   mensagens de erro.
2. Projeto de dados e migrations Flyway; dicionário de dados gerado a partir do banco.
3. Implementação do back-end: entidades, repositórios, serviços com as regras de negócio, controllers REST,
   tratamento de erros padronizado (ProblemDetail) e segurança (sessão, BCrypt, CSRF, papéis).
4. Testes automatizados de cada camada, incluindo um teste de concorrência com 10 compras simultâneas.
5. Implementação do front-end: vitrine, carrinho, login/cadastro, checkout, PIX, meus pedidos e administração.
6. Validação ponta a ponta no navegador, registro dos defeitos encontrados, correção e reexecução.
7. Publicação no Render (Docker + PostgreSQL) e gravação dos vídeos. ✏️ _confirmar_

## Resultados

- Sistema funcional com 10 casos de uso, publicado em ✏️ _link do Render_.
- **79 testes automatizados aprovados**, cobertura de **95% das linhas e 89% dos ramos**; **21/21** verificações
  do roteiro no navegador.
- **6 defeitos encontrados e corrigidos** na validação, com evidências antes/depois — o mais grave permitia que a
  edição de um produto desfizesse vendas feitas no mesmo momento (venda acima do estoque); a correção gerou a
  regra RN11.
- O teste de concorrência comprovou, ao remover o bloqueio do banco, que o problema previsto na revisão do PIT I
  era real: 10 pedidos aceitos para 5 unidades. Com o bloqueio, o estoque nunca fica negativo.
- Limitações conhecidas documentadas: PIX simulado (sem integração bancária), sem expiração automática do PIX e
  sem limite de tentativas de login.

**Links**
- Repositório: ✏️ _https://github.com/…_
- Aplicação: ✏️ _https://….onrender.com_
- Vídeo narrado: ✏️
- Vídeo das correções: ✏️
