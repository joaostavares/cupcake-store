# Situação 1 — Escopo revisado

## 1. Visão geral

**Cupcakes Gourmet** é uma loja web responsiva para venda de cupcakes artesanais. O cliente navega
pelo catálogo (sem precisar de login), monta um carrinho, faz login/cadastro ao finalizar a compra,
paga via **PIX (simulado)** e acompanha o status do pedido. Um administrador mantém o catálogo e o
estoque e movimenta os pedidos até a entrega.

O PIT I previa um "AppMobile". Como a entrega do PIT II exige front-end em HTML, CSS e JavaScript,
o produto passou a ser uma **aplicação web responsiva** (funciona no navegador do celular).

## 2. Escopo do MVP (entregue)

| # | Funcionalidade | Caso de uso |
|---|----------------|-------------|
| 1 | Vitrine pública com sabores, preços e disponibilidade | CU02 |
| 2 | Carrinho no navegador com validação de estoque | CU03 |
| 3 | Cadastro e login com senha criptografada (BCrypt) | CU04 |
| 4 | Checkout com cálculo de frete (RN02) e baixa transacional de estoque (RN01/RN05) | CU01 |
| 5 | Pagamento PIX simulado: BR Code "copia e cola" + QR Code | CU05 |
| 6 | Simulação de confirmação de pagamento | CU07 |
| 7 | "Meus pedidos" com status e resumo | CU06 |
| 8 | Cancelamento de pedido aguardando pagamento | CU10 |
| 9 | Admin: cadastrar/editar/desativar cupcakes e ajustar estoque | CU08 |
| 10 | Admin: avançar status dos pedidos | CU09 |

## 3. Fora do escopo (limitações conhecidas)

- **Integração real com PSP/banco**: o PIX é simulado. O código gerado segue o formato BR Code (EMV,
  com CRC16 válido), mas usa uma chave fictícia; a confirmação é feita por um botão que faz o papel do
  webhook do PSP.
- Expiração automática de PIX não pago (o cliente ou o admin cancela manualmente).
- Cálculo de frete por CEP/distância (substituído por regra de faixa — RN02).
- Recuperação de senha por e-mail, upload de imagens (o admin informa uma URL), notificações.
- Cupom de desconto e múltiplos endereços por cliente.

## 4. Problemas encontrados na modelagem do PIT I e como foram corrigidos

| # | Problema no PIT I | Correção no PIT II | Onde ver |
|---|-------------------|--------------------|----------|
| 1 | RN01 e RN02 citadas, nunca definidas | Regras escritas e numeradas (RN01–RN10) | [regras-de-negocio.md](regras-de-negocio.md) |
| 2 | TELA01–03 sem protótipo | Wireframes (PlantUML Salt) + telas reais + catálogo de mensagens | [ihc/](ihc/) |
| 3 | Cliente sem e-mail/senha, mas com login | `email` (único) e `senhaHash` (BCrypt) | dicionário de dados |
| 4 | ItemPedido sem preço unitário | `precoUnitario` congelado no momento da compra | RN04 |
| 5 | Dinheiro em `Double` | `BigDecimal` / `NUMERIC(10,2)` | classes / físico |
| 6 | `status: String` | enum `StatusPedido` + CHECK no banco + máquina de estados | `07-estados-pedido` |
| 7 | Sem entidade Pagamento | `Pagamento` 1:1 com Pedido (método, status, código PIX, valor, datas) | classes |
| 8 | Frete/taxa fora do Pedido | `subtotal`, `frete`, `total` no Pedido (CHECK total = subtotal + frete) | físico |
| 9 | Sem administrador | Ator Administrador, papel `ADMIN`, CU08 e CU09 | casos de uso |
| 10 | Estoque validado só no carrinho | Revalidação e baixa no checkout com `SELECT ... FOR UPDATE` dentro da transação | RN05, sequência |
| 11 | Métodos de negócio nas entidades | Regras movidas para `PedidoService`, `CalculadoraFrete`, `PixService` | `03-classes-camadas` |
| 12 | Login exigido para ver a vitrine | Vitrine e carrinho públicos; login apenas no checkout | CU02/CU03 |
| 13 | Participante "AppMobile" | "Navegador (Web responsiva)" | sequência |
| 14 | Faltavam casos de uso | CU06 Acompanhar Pedido, CU07 Confirmar Pagamento, CU10 Cancelar Pedido | casos de uso |

Decisão de modelagem adicional: a classe `Cliente` do PIT I virou **`Usuario`** com atributo `papel`
(`CLIENTE` ou `ADMIN`). Assim o administrador reaproveita o mesmo cadastro e mecanismo de login, sem
duplicar tabelas. Os dados de `Cliente` do PIT I (nome, telefone, endereço) foram mantidos.

## 5. Artefatos desta situação

- [Regras de negócio](regras-de-negocio.md)
- [Casos de uso expandidos](casos-de-uso.md)
- Diagramas UML (`.puml` + `.png`): [uml/](uml/)
- IHC — wireframes e mensagens: [ihc/](ihc/)
- Projeto de dados (conceitual, lógico, físico, dicionário): [dados/](dados/)
