# Registro de Mudanças

## A. Mudanças em relação ao planejamento do PIT I (Situação 1)

Resumo — detalhes e justificativas em [../situacao-1/escopo.md](../situacao-1/escopo.md#4-problemas-encontrados-na-modelagem-do-pit-i-e-como-foram-corrigidos).

| Antes (PIT I) | Depois (PIT II) |
|---------------|-----------------|
| App mobile | Web responsiva (HTML/CSS/JS) |
| Login exigido para ver a vitrine | Vitrine e carrinho públicos; login no checkout |
| `Cliente` sem e-mail/senha | `Usuario` com e-mail único, senha BCrypt e papel (CLIENTE/ADMIN) |
| Sem administrador | Ator Administrador, CU08 e CU09 |
| `Double` para dinheiro | `BigDecimal` / `NUMERIC(10,2)` |
| `status: String` | Enum + CHECK + máquina de estados (RN07) |
| Sem `Pagamento` | Entidade `Pagamento` 1:1 com PIX (BR Code + QR Code) |
| `ItemPedido` sem preço | `precoUnitario` congelado (RN04) |
| Pedido sem frete | `subtotal`, `frete`, `total` (RN02) |
| Estoque validado só no carrinho | Revalidação + baixa transacional com lock (RN05) |
| Regras nos métodos das entidades | Regras em services (MVC em camadas) |
| RN01/RN02 citadas, não definidas | RN01–RN11 documentadas e testadas |
| TELA01–03 sem protótipo | Wireframes TELA01–07 + telas reais + catálogo de mensagens |
| 4 casos de uso | 10 casos de uso (CU06 Acompanhar, CU07 Confirmar Pagamento, CU10 Cancelar…) |

## B. Mudanças feitas na validação (Situação 3)

| Data | Commit | Defeito | Mudança | Arquivos | Documentação atualizada |
|------|--------|---------|---------|----------|-------------------------|
| 17/11/2026 | `554491d` | D01 | Roteiro E2E aguarda o QR Code carregar | `scripts/e2e-capturas.mjs` | plano de testes (CT08) |
| 17/11/2026 | `554491d` | D02 | `object-fit: contain` nas imagens | `static/css/style.css` | — |
| 17/11/2026 | `554491d` | D03 | Estilo de títulos de painel, fieldset e select | `static/css/style.css` | — |
| 17/11/2026 | `554491d` | D04 | Carrinho sincroniza preço/estoque com a cotação do servidor | `static/js/carrinho.js`, `static/js/pagina-carrinho.js` | plano de testes (CT21) |
| 17/11/2026 | `554491d` | D05 | Edição de cupcake não altera estoque; campo bloqueado na edição; nova RN11 | `CupcakeService`, `CupcakeRequest`, `static/admin.html`, `static/js/admin.js`, `AdminEstoqueIT`, `CupcakeServiceTest` | regras de negócio (RN11), arquitetura (API), manual de uso (2.1), diagrama de classes de camadas |
| 17/11/2026 | `554491d` | D06 | Build Docker com imagem Maven oficial | `Dockerfile` | README |
| 17/11/2026 | (este commit) | — | Diagrama de classes de camadas alinhado aos nomes reais dos métodos (`definirEstoque`, `findByIdForUpdate`…) | `docs/situacao-1/uml/03-classes-camadas.puml` | — |

## C. Histórico de versões

| Versão | Commit | Conteúdo |
|--------|--------|----------|
| 0.1 | `999188b` | Scaffold |
| 0.2 | `eaab45d` | Situação 1: documentação, UML, dados, migrations |
| 0.3 | `446b3f4` | Back-end MVC completo |
| 0.4 | `527357b` | Testes unitários e de integração |
| 0.5 | `91b86ff` | Front-end — **versão avaliada no laudo** |
| 1.0 | `554491d` | Correções D01–D06 — **versão corrigida** |
