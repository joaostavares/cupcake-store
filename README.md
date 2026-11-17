# Cupcakes Gourmet

Loja web de cupcakes artesanais com vitrine, carrinho, checkout e pagamento **PIX simulado**.
Projeto Integrador Transdisciplinar em Engenharia de Software II — Cruzeiro do Sul Virtual.
Autor: **João Serafini Tavares**.

| | |
|---|---|
| **Aplicação hospedada** | _a preencher após o deploy no Render_ |
| **Vídeo narrado (≥ 5 min)** | _a preencher_ |
| **Vídeo das correções (≤ 5 min)** | _a preencher_ |

## Tabela de codificação

| Item | Valor |
|------|-------|
| Linguagem do back-end | Java 21 (Spring Boot 3.5, arquitetura MVC em camadas) |
| Front-end | HTML5, CSS3 e JavaScript (ES modules, sem framework) |
| Banco de dados | PostgreSQL 16 (schema versionado com Flyway) |
| Hospedagem | Render (Web Service Docker + PostgreSQL gerenciado) |
| Plataforma | Web responsiva (desktop e celular) |
| Modo | Tradicional |
| Testes | JUnit 5, Mockito, MockMvc, Testcontainers, Playwright (E2E), JaCoCo |

## Contas de demonstração

| Perfil | E-mail | Senha |
|--------|--------|-------|
| Cliente | `cliente@cupcakes.com` | `Cliente@123` |
| Administrador | `admin@cupcakes.com` | `Admin@123` (sobrescrevível por `ADMIN_PASSWORD`) |

## Documentação

| Situação | Conteúdo |
|----------|----------|
| [Situação 1 — Revisão do planejamento](docs/situacao-1/escopo.md) | [escopo e problemas corrigidos](docs/situacao-1/escopo.md), [regras de negócio](docs/situacao-1/regras-de-negocio.md), [casos de uso](docs/situacao-1/casos-de-uso.md), [UML](docs/situacao-1/uml/), [IHC/wireframes/mensagens](docs/situacao-1/ihc/), [dados + dicionário](docs/situacao-1/dados/) |
| [Situação 2 — Construção](docs/situacao-2/) | [manual de uso](docs/situacao-2/manual-de-uso.md), [arquitetura e API](docs/situacao-2/arquitetura.md) |
| [Situação 3 — Validação](docs/situacao-3/) | [plano de testes](docs/situacao-3/plano-de-testes.md), [laudo de qualidade](docs/situacao-3/laudo-de-qualidade.md), [mudanças](docs/situacao-3/mudancas.md), [evidências](docs/situacao-3/evidencias/) |
| Entrega | [rascunho do Projeto de Intervenção](docs/projeto-de-intervencao.md) |

## Como rodar localmente

Pré-requisito: **JDK 21**. O Maven vem embutido (`./mvnw`).

### Opção A — sem banco instalado (H2 em memória)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```
Acesse http://localhost:8080. Os dados somem ao parar a aplicação.

### Opção B — com PostgreSQL via Docker (igual à produção)
```bash
docker compose up -d postgres
./mvnw spring-boot:run
```
Ou tudo em containers: `docker compose --profile app up --build`.

### Opção C — com um PostgreSQL já instalado
```bash
createuser -P cupcakes          # senha: cupcakes
createdb -O cupcakes cupcakes
./mvnw spring-boot:run
```
Variáveis aceitas: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PARAMS` (ex.: `?sslmode=require`),
`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DEMO_CLIENTE` (`false` para não criar o cliente demo), `PORT`.

### Testes
```bash
./mvnw test                     # unitários + integração (H2). Relatório: target/site/jacoco/index.html
./mvnw surefire-report:report-only   # relatório HTML: target/reports/surefire.html

# Testes no PostgreSQL real: automáticos se houver Docker (Testcontainers), ou:
PG_TEST_URL=jdbc:postgresql://localhost:5432/cupcakes_test ./mvnw test

# E2E no navegador + capturas de tela (app rodando em :8080 com banco novo)
npm i playwright@1.65.1 && npx playwright install chromium-headless-shell
node scripts/e2e-capturas.mjs
```

### Regerar diagramas e dicionário de dados
```bash
java -jar plantuml.jar -charset UTF-8 -tpng docs/situacao-1/**/*.puml       # PlantUML ≥ 1.2024 (layout smetana, sem Graphviz)
PGHOST=localhost PGUSER=cupcakes PGPASSWORD=cupcakes PGDATABASE=cupcakes scripts/gerar-dicionario.sh
```

## Deploy (Render)

1. Faça login no [Render](https://render.com) com a conta do GitHub.
2. **New → Blueprint** → selecione este repositório. O `render.yaml` cria o banco `cupcakes-db` e o serviço web
   `cupcakes-gourmet` (Docker) já ligados.
3. (Recomendado) Em *Environment* do serviço, defina `ADMIN_PASSWORD`.
4. Aguarde o build (~5 min). O health check é `/actuator/health`.

Plano gratuito: o serviço "dorme" após 15 min sem acesso (primeiro acesso leva ~1 min) e o PostgreSQL gratuito
do Render expira após 30 dias. Alternativa sem expiração: criar um Postgres no [Neon](https://neon.tech) e
configurar `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `DB_PARAMS=?sslmode=require` no serviço.

## Decisões tomadas (ambiguidades resolvidas)

| # | Decisão | Motivo |
|---|---------|--------|
| 1 | **RN02:** frete grátis a partir de R$ 100,00; abaixo, R$ 10,00 fixo; sem taxa de serviço | O PIT I citava "frete e taxas" sem valores. Regra simples, testável e comum no varejo. |
| 2 | `Cliente` virou **`Usuario` com papel** (CLIENTE/ADMIN) | Administrador reaproveita cadastro e login, sem tabela duplicada. |
| 3 | Limite de **20 unidades por sabor** por pedido (RN03) | Evita pedidos absurdos/abuso de estoque no ambiente público. |
| 4 | **Bloqueio pessimista** no checkout | Conflito é provável nos itens populares; ver [arquitetura](docs/situacao-2/arquitetura.md). |
| 5 | **Sessão com cookie + CSRF** em vez de JWT | Front e API na mesma origem; logout de verdade; menos código. |
| 6 | Confirmação do PIX por **botão "Simular pagamento"** (dono do pedido ou admin) | Faz o papel do webhook do PSP; integração real fora do escopo. |
| 7 | BR Code com **CRC16 válido** e QR Code real, chave fictícia | Demonstra o formato correto sem movimentar dinheiro. |
| 8 | **Carrinho no localStorage**, cotação de valores sempre no servidor | Visitante monta o carrinho sem login; o servidor é a fonte da verdade dos preços. |
| 9 | Cliente só **cancela antes do pagamento**; admin cancela até "Pago" | Depois de pago envolve estorno, que é manual. |
| 10 | Cupcakes **não são excluídos, só desativados** | Preserva o histórico de pedidos (FK). |
| 11 | Acesso a pedido alheio responde **404** | Não revela a existência do pedido. |
| 12 | Admin e cliente demo criados **na inicialização**, não em migration | A senha de produção vem de variável de ambiente, não do repositório. Sem `ADMIN_PASSWORD`, usa a senha documentada (aceitável para demo acadêmica). |
| 13 | **H2 (modo PostgreSQL)** nos testes e no perfil `h2` | Suíte roda em qualquer máquina; Postgres real coberto por Testcontainers/`PG_TEST_URL`. |
| 14 | Imagens dos cupcakes como **SVG próprios**; admin informa URL | Sem direitos autorais de terceiros; upload de arquivo fora do escopo. |
| 15 | Expiração automática de PIX **fora do escopo** | Cancelamento manual cobre o caso; documentado como limitação. |

## Estrutura

```
docs/                 documentação por situação (1, 2, 3)
scripts/              gerador do dicionário de dados e roteiro E2E
src/main/java/...     controller · service · repository · model · dto · exception · config · security
src/main/resources/
  db/migration/       migrations Flyway (projeto físico)
  static/             front-end (HTML/CSS/JS)
src/test/java/...     testes unitários e de integração
Dockerfile, docker-compose.yml, render.yaml
```
