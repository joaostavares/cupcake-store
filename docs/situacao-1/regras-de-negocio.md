# Regras de Negócio

Cada regra indica onde está implementada e qual teste automatizado a cobre.

| ID | Regra | Implementação | Teste |
|----|-------|---------------|-------|
| **RN01** | **Disponibilidade de estoque.** Só é possível adicionar ao carrinho ou comprar uma quantidade menor ou igual ao estoque atual de um cupcake **ativo**. Cupcakes com estoque 0 aparecem na vitrine como "Esgotado", sem botão de compra. | Front: `carrinho.js` (ao adicionar). Back: `PedidoService.criarPedido` | `PedidoServiceTest`, `PedidoApiIT` |
| **RN02** | **Frete.** Subtotal **≥ R$ 100,00 → frete grátis**. Abaixo disso, frete fixo de **R$ 10,00**. Total = subtotal + frete. Não há taxa de serviço. Cálculo sempre feito no servidor (o front só exibe a cotação). | `CalculadoraFrete` | `CalculadoraFreteTest` |
| **RN03** | **Limites do pedido.** Cada item tem quantidade entre 1 e 20; um pedido tem ao menos 1 item; o mesmo cupcake não se repete em itens diferentes (quantidades são somadas). | DTO `ItemPedidoRequest` (Bean Validation) + `PedidoService` | `PedidoServiceTest`, `PedidoApiIT` |
| **RN04** | **Preço congelado.** O `precoUnitario` do item é copiado do cupcake no momento da compra. Alterar o preço do cupcake não altera pedidos existentes. | `PedidoService.criarPedido` | `PedidoServiceTest` |
| **RN05** | **Baixa transacional de estoque.** No checkout o estoque é revalidado e decrementado na mesma transação que grava o pedido, com bloqueio pessimista das linhas (`SELECT ... FOR UPDATE`). Se qualquer item faltar, nada é gravado. | `CupcakeRepository.findAllByIdForUpdate` + `@Transactional` | `PedidoServiceTest`, `CheckoutConcorrenciaPostgresIT` |
| **RN06** | **Cadastro.** E-mail é único (case-insensitive, armazenado em minúsculas), senha com no mínimo 6 caracteres, armazenada com BCrypt. | `UsuarioService` | `UsuarioServiceTest`, `AuthApiIT` |
| **RN07** | **Ciclo de vida do pedido.** `AGUARDANDO_PAGAMENTO → PAGO → EM_PREPARO → ENVIADO → ENTREGUE`. Cancelamento só a partir de `AGUARDANDO_PAGAMENTO` (cliente ou admin) ou `PAGO` (somente admin). Transições fora disso são rejeitadas (HTTP 422). | `StatusPedido.podeMudarPara`, `PedidoService` | `StatusPedidoTest`, `PedidoServiceTest` |
| **RN08** | **Cancelamento devolve estoque.** Ao cancelar, as quantidades dos itens voltam ao estoque e o pagamento passa a `CANCELADO`. | `PedidoService.cancelar` | `PedidoServiceTest` |
| **RN09** | **Pagamento PIX.** Todo pedido nasce com um pagamento `PENDENTE` no valor do total e um código BR Code. A confirmação muda pagamento para `CONFIRMADO` e o pedido para `PAGO`; só é aceita se o pedido estiver `AGUARDANDO_PAGAMENTO`. | `PedidoService.confirmarPagamento`, `PixService` | `PixServiceTest`, `PedidoServiceTest` |
| **RN10** | **Privacidade e papéis.** Catálogo é público. Cliente só vê e altera os próprios pedidos. Apenas `ADMIN` gerencia catálogo, estoque e status. Cupcakes nunca são excluídos — são desativados (preserva o histórico de pedidos). | `SecurityConfig`, `PedidoService` | `SegurancaApiIT` |
