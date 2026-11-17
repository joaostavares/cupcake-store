# Cupcakes Gourmet — PIT Engenharia de Software II

Projeto da faculdade (Cruzeiro do Sul Virtual, EAD). Disciplina: Projeto Integrador Transdisciplinar em Engenharia de Software II.
Objetivo: implementar o sistema planejado no PIT I (loja de cupcakes com pedido e pagamento PIX), com testes e documentação, tudo no GitHub.

Autor: João Serafini Tavares. Dev Java/Spring pleno — não explicar o básico de Spring, Maven, Git etc.

---

## 1. O que a disciplina exige (3 situações-problema)

### Situação 1 — Revisar o planejamento
- Revisar o escopo/ideia do PIT I.
- Revisar os diagramas UML (casos de uso, classes, sequência, atividades se fizer sentido).
- Revisar a IHC: telas, mensagens de erro, protótipo.
- Projeto de dados: conceitual → lógico normalizado → **físico** (SGBD popular), com **dicionário de dados**.

### Situação 2 — Construir
- Repositório no GitHub (público; vira portfólio).
- Front-end em HTML, CSS e JS.
- Back-end em **MVC**, linguagem Java.
- Testes unitários (JUnit), tratamento de erros, tudo funcional.
- Entregáveis: código front + back, **manual de uso**, vídeo narrado.

### Situação 3 — Validar e fechar
- Documentar testes de verificação e validação (comportamento esperado vs obtido).
- Corrigir os problemas encontrados e registrar as mudanças na documentação.
- Pastas no Git por situação + código.
- Vídeo mostrando as mudanças adotadas. Falhas não devem ser escondidas — fazem parte da avaliação.

### Documento de entrega deste semestre (as 3 situações são obrigatórias)
- **Documentação:** link do repositório Git com a documentação revisada.
- **Tabela de codificação:** linguagem do back-end, banco de dados, hospedagem, plataforma, modo (Tradicional), link do GitHub, **link da solução em funcionamento (hospedada)**, link do vídeo narrado (**no mínimo 5 min**).
- **Laudo de qualidade:** erros encontrados e correções, **com evidências**.
- **Segundo vídeo** (até 5 min): as correções feitas.

O laudo de qualidade sai dos testes automatizados (relatório JUnit + JaCoCo) e de um roteiro de teste manual executado pelo próprio João, com prints como evidência.

**PRAZO: a entrega é HOJE.** Prioridade máxima é um MVP hospedado e funcional o quanto antes. Escopo mínimo: vitrine, cadastro/login, carrinho, checkout com PIX simulado, resumo do pedido, admin simples de estoque. Documentação UML/dados em paralelo, enxuta.

---

## 2. Modelagem herdada do PIT I (estado atual)

Os diagramas originais foram feitos em PlantUML (só existem como imagem). Recriar como `.puml` em `docs/situacao-1/uml/`.

**Casos de uso** (sistema "App Cupcakes Gourmet"):
- Ator Cliente: Realizar Cadastro/Login, Visualizar Catálogo, Adicionar ao Carrinho, Finalizar Pedido.
- Finalizar Pedido `<<include>>` Processar Pagamento ← ator secundário Sistema de Pagamento.

**Classes:**
- `Cliente` (id: Long, nome, telefone, endereco: String; cadastrar(), fazerLogin()) 1 —realiza→ * `Pedido`
- `Pedido` (id: Long, dataHora: Date, status: String, total: Double; calcularTotal(), processarPagamento()) 1 ◆—possui→ * `ItemPedido`
- `ItemPedido` (id: Long, quantidade: Integer, subtotal: Double) * —contém→ 1 `Cupcake`
- `Cupcake` (id: Long, sabor: String, preco: Double, estoque: Integer; atualizarEstoque())

**Sequência (finalizar compra):** Cliente → AppMobile "Finalizar Compra" → `POST /pedidos` (dados do carrinho) → ControladorPedido: verificarEstoque() no BD → calcularTotal() → persistirPedido() → `201 Created` → app exibe chave PIX.

**CU01 — Realizar Pedido de Cupcakes**
- Pré: cliente cadastrado e logado. Pós: pedido registrado aguardando confirmação de pagamento.
- Fluxo: TELA01 Vitrine lista cupcakes e preços → cliente escolhe e ajusta quantidade → "Adicionar ao Carrinho" → valida estoque (**RN01**) → carrinho → "Finalizar Compra" → total com frete e taxas (**RN02**) → TELA02 Pagamento → PIX → registra pedido, gera código de pagamento, TELA03 Resumo do Pedido.
- Alternativo Alfa (sem estoque): avisa o usuário e volta à seleção.

---

## 3. Problemas encontrados na modelagem (corrigir na Situação 1 — é exatamente o que a disciplina pede)

1. **RN01 e RN02 são citadas mas nunca definidas.** Escrever as regras de negócio (ex.: RN02 = frete fixo ou por faixa + taxa).
2. **TELA01–03 citadas sem protótipo.** Criar wireframes ou telas reais e documentar as mensagens de erro.
3. **Cliente sem email/senha**, mas existe login. Adicionar email (único) e senhaHash.
4. **ItemPedido sem precoUnitario.** O preço precisa ser congelado no momento da compra, senão mudar o preço do cupcake altera pedidos antigos.
5. **Dinheiro em Double** → BigDecimal / NUMERIC(10,2).
6. **status: String** → enum (`AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`).
7. **Sem entidade Pagamento**, embora existam PIX e "Sistema de Pagamento". Criar `Pagamento` (pedido 1:1, método, status, codigoPix, valor, datas).
8. **Frete/taxa não aparecem no Pedido.** Adicionar subtotal, frete e total.
9. **Sem administrador.** Ninguém cadastra cupcake nem repõe estoque. Adicionar ator Administrador + caso de uso "Gerenciar Catálogo/Estoque" (ou justificar por que ficou fora do escopo).
10. **Estoque validado só ao adicionar ao carrinho.** Revalidar e baixar o estoque na finalização, dentro da transação (lock otimista/pessimista), para não vender o que acabou entre o carrinho e o checkout.
11. **Métodos de negócio nas entidades** (processarPagamento() em Pedido) não combinam com MVC em camadas → mover para services. Atualizar o diagrama de classes de acordo.
12. **Pré-condição exige login para ver a vitrine.** Catálogo deve ser público; login só no checkout.
13. **Sequência diz "AppMobile"**, mas a entrega pede HTML/CSS/JS → web responsiva. Renomear o participante.
14. **Faltam casos de uso** razoáveis: "Acompanhar Pedido" (cliente), "Confirmar Pagamento" (sistema).

---

## 4. Stack e decisões

- **Java 21 + Spring Boot 3**, Maven.
- **Back MVC em camadas:** controller → service → repository → model (JPA). DTOs na borda; validação com Bean Validation; `@ControllerAdvice` para erros padronizados (ProblemDetail).
- **Front:** HTML + CSS + JS puro (sem framework), servido como estático pelo próprio Spring Boot, consumindo a API REST. Combina com o diagrama de sequência (`POST /pedidos` → 201). Um único deploy.
- **Banco:** PostgreSQL. Schema via **Flyway** — as migrations são o projeto físico versionado; o dicionário de dados é gerado/derivado delas.
- **Auth:** Spring Security com sessão (cookie) e BCrypt. Papéis CLIENTE e ADMIN.
- **Pagamento PIX: simulado.** Gera um código fake (pode ser um BR Code com valor) e um endpoint/botão "simular confirmação de pagamento" que muda o status. Integração real com PSP fica fora do escopo — documentar como limitação conhecida.
- **Testes:** JUnit 5 + Mockito (services), MockMvc (controllers), Testcontainers com Postgres (integração/repositories). Jacoco para relatório de cobertura (bom para a documentação).
- **Ambiente local:** `docker-compose.yml` com Postgres.
- **Deploy:** Render (tudo num container só: Spring servindo o front + Postgres). Dockerfile pronto para isso. O template exige link da solução funcionando. GitHub Pages não serve (só estático).

---

## 5. Estrutura do repositório

```
/
├── CLAUDE.md
├── README.md                 # visão geral, como rodar, link do deploy, link do vídeo
├── docs/
│   ├── situacao-1/           # revisão do planejamento
│   │   ├── escopo.md         # escopo revisado + o que mudou do PIT I e por quê
│   │   ├── regras-de-negocio.md
│   │   ├── casos-de-uso.md   # CU expandidos
│   │   ├── uml/*.puml (+ PNGs gerados)
│   │   ├── ihc/              # wireframes/telas, mensagens de erro
│   │   └── dados/            # conceitual, lógico, físico, dicionario-de-dados.md
│   ├── situacao-2/
│   │   ├── manual-de-uso.md
│   │   └── arquitetura.md
│   └── situacao-3/
│       ├── plano-de-testes.md        # casos de teste V&V: esperado x obtido
│       ├── laudo-de-qualidade.md     # erros, correções, evidências
│       └── mudancas.md
├── src/                      # Spring Boot (main + test)
├── docker-compose.yml
└── Dockerfile
```

---

## 6. Ordem de execução sugerida

1. Scaffold do projeto + docker-compose + estrutura de pastas + README inicial.
2. Situação 1: escopo revisado, regras de negócio, UML corrigida em PlantUML, modelo de dados, migrations Flyway, dicionário de dados.
3. Back-end: entidades → repositories → services (regras RN01/RN02, transação do checkout) → controllers → tratamento de erros → segurança.
4. Testes unitários e de integração junto com cada camada.
5. Front: vitrine, carrinho (localStorage), login/cadastro, checkout, tela PIX/resumo, meus pedidos, admin de catálogo/estoque.
6. Seed de dados (cupcakes de exemplo) via migration.
7. Manual de uso, plano de testes.
8. Deploy no Render, o mais cedo possível.
9. Roteiro de teste manual, laudo de qualidade, correções + documentação das mudanças.
10. Rascunho do documento "Projeto de Intervenção" (objetivo, revisão de conceito, metodologia, procedimento, resultado) a partir do que foi feito. O campo **Tempo** quem preenche é o João.

## 7. Só o João pode fazer
- Criar/usar a conta GitHub e a conta de deploy.
- Gravar e narrar o vídeo.
- Preencher o tempo gasto e revisar o texto final do documento de entrega.
