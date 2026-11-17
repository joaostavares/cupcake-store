# Casos de Uso Expandidos

Diagrama: [uml/casos-de-uso.png](uml/casos-de-uso.png) · Mensagens (MSGxx): [ihc/mensagens.md](ihc/mensagens.md)

Atores: **Visitante** (não autenticado), **Cliente** (autenticado, papel CLIENTE), **Administrador**
(papel ADMIN), **Sistema de Pagamento** (PSP — simulado).

---

## CU01 — Realizar Pedido de Cupcakes (Finalizar Compra)

- **Ator principal:** Cliente
- **Pré-condição:** carrinho com ao menos 1 item. *(Revisado: no PIT I exigia login desde o início; agora o login é pedido apenas aqui.)*
- **Pós-condição:** pedido registrado com status `AGUARDANDO_PAGAMENTO`, estoque baixado e pagamento PIX `PENDENTE` gerado.

**Fluxo principal**
1. Na TELA01 (Vitrine) o cliente escolhe cupcakes e quantidades e clica "Adicionar ao carrinho" (CU03; valida RN01).
2. Na TELA05 (Carrinho) o sistema mostra itens, subtotal, frete e total (RN02).
3. O cliente clica "Finalizar compra".
4. Se não estiver logado, o sistema redireciona para a TELA04 (Login/Cadastro) e depois volta ao checkout (CU04).
5. Na TELA02 (Checkout) o sistema exibe o resumo e o endereço de entrega (pré-preenchido do cadastro).
6. O cliente confirma o endereço e clica "Pagar com PIX".
7. O sistema revalida o estoque, congela os preços (RN04), baixa o estoque (RN05), calcula o frete (RN02) e registra o pedido.
8. O sistema inclui CU05 (Processar Pagamento PIX) e exibe a TELA03 (Pagamento PIX + Resumo do Pedido).
9. O carrinho é esvaziado.

**Fluxo alternativo A1 — Sem estoque (passo 7)**
1. Algum item não tem mais estoque suficiente (outro cliente comprou entre o carrinho e o checkout).
2. O sistema não grava nada e exibe MSG05 informando o sabor, a quantidade disponível e a solicitada.
3. O cliente volta ao carrinho para ajustar as quantidades.

**Fluxo alternativo A2 — Cupcake desativado (passo 7):** exibe MSG11 e volta ao carrinho.

**Fluxo de exceção E1 — Sessão expirada:** a API responde 401, o sistema exibe MSG06 e leva ao login.

---

## CU02 — Visualizar Catálogo
- **Ator:** Visitante. **Pré:** nenhuma.
- **Fluxo:** o sistema lista os cupcakes ativos com imagem, sabor, descrição, preço e disponibilidade. Itens com estoque 0 aparecem como "Esgotado".

## CU03 — Gerenciar Carrinho
- **Ator:** Visitante. O carrinho fica no navegador (localStorage), sem exigir login.
- **Fluxo:** adicionar item (valida RN01 contra o estoque exibido; senão MSG04), alterar quantidade (1–20, RN03), remover item, ver cotação de frete/total calculada pelo servidor (`POST /api/pedidos/cotacao`).

## CU04 — Realizar Cadastro/Login
- **Ator:** Visitante.
- **Cadastro:** informa nome, e-mail, senha, telefone e endereço. Valida RN06. E-mail repetido → MSG02; campos inválidos → MSG03. Após o cadastro o usuário já fica logado.
- **Login:** e-mail e senha. Credenciais inválidas → MSG01.
- **Logout:** encerra a sessão.

## CU05 — Processar Pagamento PIX (incluído por CU01)
- **Atores:** Cliente, Sistema de Pagamento.
- **Fluxo:** o sistema cria o `Pagamento` (`PENDENTE`, valor = total do pedido) e gera o código BR Code "copia e cola" e o QR Code. A TELA03 exibe ambos com o botão "Copiar código".

## CU06 — Acompanhar Pedido
- **Ator:** Cliente.
- **Fluxo:** em "Meus pedidos" (TELA06) o cliente vê seus pedidos (mais recentes primeiro) com data, total e status; ao abrir um pedido vê itens, valores, endereço e pagamento. Acesso ao pedido de outro cliente → MSG08 (404, para não revelar a existência).

## CU07 — Confirmar Pagamento
- **Ator:** Sistema de Pagamento (simulado pelo botão "Simular pagamento PIX", disponível ao dono do pedido e ao admin).
- **Fluxo:** pedido `AGUARDANDO_PAGAMENTO` → `PAGO`; pagamento → `CONFIRMADO` com data/hora (RN09). Outro status → MSG09.

## CU08 — Gerenciar Catálogo e Estoque
- **Ator:** Administrador.
- **Fluxo:** na TELA07 (Admin) lista todos os cupcakes (inclusive inativos); cadastra (sabor único, preço > 0, estoque ≥ 0), edita, ativa/desativa (RN10) e define o novo estoque.
- **Exceções:** sabor repetido → MSG02b; dados inválidos → MSG03.

## CU09 — Atualizar Status do Pedido
- **Ator:** Administrador.
- **Fluxo:** na aba "Pedidos" da TELA07, vê todos os pedidos (filtro por status) e avança o status conforme RN07. Transição inválida → MSG09.

## CU10 — Cancelar Pedido
- **Atores:** Cliente (próprio pedido `AGUARDANDO_PAGAMENTO`), Administrador (`AGUARDANDO_PAGAMENTO` ou `PAGO`).
- **Fluxo:** pedido → `CANCELADO`, pagamento → `CANCELADO`, estoque devolvido (RN08).
