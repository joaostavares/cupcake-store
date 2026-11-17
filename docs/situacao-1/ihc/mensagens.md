# Catálogo de Mensagens (IHC)

Toda resposta de erro da API segue o padrão **ProblemDetail (RFC 9457)**: `status`, `title`, `detail`
e, em erros de validação, `erros` (campo → mensagem). O front exibe o `detail` em um alerta no topo
do formulário/tela e, nos erros de validação, destaca cada campo com a sua mensagem.

Princípios: linguagem simples, sem jargão técnico; dizer o que aconteceu **e** o que fazer;
nunca expor stack trace; cores + texto (não depender só de cor).

| ID | Situação | Texto exibido | HTTP | Tela |
|----|----------|---------------|------|------|
| MSG01 | Login inválido | "E-mail ou senha inválidos." | 401 | TELA04 |
| MSG02 | E-mail já cadastrado | "Este e-mail já está cadastrado. Faça login ou use outro e-mail." | 409 | TELA04 |
| MSG02b | Sabor duplicado (admin) | "Já existe um cupcake com o sabor '{sabor}'." | 409 | TELA07 |
| MSG03 | Campos inválidos | "Verifique os campos destacados." + mensagem por campo (ex.: "A senha deve ter no mínimo 6 caracteres") | 400 | todas com formulário |
| MSG04 | Adicionar além do estoque | "Só temos {n} unidade(s) de {sabor} disponíveis." | — (front) | TELA01/TELA05 |
| MSG05 | Estoque acabou no checkout | "Estoque insuficiente para {sabor}: disponível {d}, solicitado {s}." | 409 | TELA02 |
| MSG06 | Não autenticado | "Faça login para continuar." | 401 | qualquer |
| MSG07 | Sem permissão | "Acesso restrito ao administrador." | 403 | TELA07 |
| MSG08 | Recurso inexistente | "Pedido não encontrado." / "Cupcake não encontrado." | 404 | TELA03/TELA06/TELA07 |
| MSG09 | Transição de status inválida | "Não é possível mudar o pedido de {atual} para {novo}." | 422 | TELA03/TELA07 |
| MSG10 | Carrinho vazio | "Seu carrinho está vazio." | 400 | TELA05/TELA02 |
| MSG11 | Cupcake desativado | "O cupcake {sabor} não está mais disponível." | 409 | TELA02 |
| MSG12 | Erro inesperado | "Ocorreu um erro inesperado. Tente novamente em instantes." | 500 | qualquer |
| MSG13 | Sucessos | "Adicionado ao carrinho", "Pagamento confirmado!", "Pedido cancelado", "Cupcake salvo" | 2xx | várias |
