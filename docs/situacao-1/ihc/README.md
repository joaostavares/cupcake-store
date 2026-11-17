# IHC — Interface Humano-Computador

No PIT I as telas TELA01–TELA03 eram citadas no caso de uso, mas não havia protótipo. Nesta revisão:

1. **Wireframes** de baixa fidelidade feitos em PlantUML Salt (fonte `.puml` versionada, imagem `.png`).
2. **Telas reais** implementadas em HTML/CSS/JS (`src/main/resources/static`). Capturas de tela entram
   em `docs/situacao-3/evidencias/` durante o roteiro de teste manual.
3. **Catálogo de mensagens**: [mensagens.md](mensagens.md).

| Tela | Wireframe | Página real |
|------|-----------|-------------|
| TELA01 Vitrine (pública) | [tela01-vitrine.png](tela01-vitrine.png) | `/index.html` |
| TELA02 Checkout | [tela02-checkout.png](tela02-checkout.png) | `/checkout.html` |
| TELA03 Pagamento PIX + Resumo | [tela03-pix-resumo.png](tela03-pix-resumo.png) | `/pedido.html?id=` |
| TELA04 Login / Cadastro | [tela04-login.png](tela04-login.png) | `/login.html` |
| TELA05 Carrinho | [tela05-carrinho.png](tela05-carrinho.png) | `/carrinho.html` |
| TELA06 Meus Pedidos | [tela06-meus-pedidos.png](tela06-meus-pedidos.png) | `/meus-pedidos.html` |
| TELA07 Administração | [tela07-admin.png](tela07-admin.png) | `/admin.html` |

## Diretrizes aplicadas
- **Responsivo (mobile first):** grade de cards que vira coluna única em telas estreitas; o PIT I
  previa app mobile, então o uso no celular continua sendo o cenário principal.
- **Visibilidade do estado do sistema (Nielsen):** contador do carrinho no cabeçalho, status do pedido
  com cor + texto, botão desabilitado durante o envio ("Processando...").
- **Prevenção de erros:** botão de compra some para itens esgotados; quantidade limitada ao estoque e
  a 20 (RN03); confirmação antes de cancelar pedido.
- **Mensagens claras:** ver catálogo de mensagens; erros de campo aparecem junto ao campo.
- **Acessibilidade:** `label` em todos os campos, contraste AA, alertas com `role="alert"`, foco visível.
