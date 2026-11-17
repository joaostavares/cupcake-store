-- Descrições usadas para gerar o dicionário de dados (scripts/gerar-dicionario.sh).
COMMENT ON TABLE usuario IS 'Pessoas que acessam o sistema autenticadas: clientes e administradores.';
COMMENT ON COLUMN usuario.id IS 'Identificador sequencial.';
COMMENT ON COLUMN usuario.nome IS 'Nome completo.';
COMMENT ON COLUMN usuario.email IS 'E-mail de login, armazenado em minúsculas, único (RN06).';
COMMENT ON COLUMN usuario.senha_hash IS 'Hash BCrypt da senha; a senha em texto nunca é gravada.';
COMMENT ON COLUMN usuario.telefone IS 'Telefone de contato (opcional).';
COMMENT ON COLUMN usuario.endereco IS 'Endereço padrão de entrega (opcional).';
COMMENT ON COLUMN usuario.papel IS 'CLIENTE ou ADMIN (RN10).';
COMMENT ON COLUMN usuario.criado_em IS 'Data/hora do cadastro.';

COMMENT ON TABLE cupcake IS 'Produtos do catálogo.';
COMMENT ON COLUMN cupcake.id IS 'Identificador sequencial.';
COMMENT ON COLUMN cupcake.sabor IS 'Nome do sabor, único.';
COMMENT ON COLUMN cupcake.descricao IS 'Descrição exibida na vitrine.';
COMMENT ON COLUMN cupcake.preco IS 'Preço unitário atual em reais (> 0).';
COMMENT ON COLUMN cupcake.estoque IS 'Unidades disponíveis (>= 0). Baixado no checkout (RN05).';
COMMENT ON COLUMN cupcake.imagem_url IS 'Caminho ou URL da imagem.';
COMMENT ON COLUMN cupcake.ativo IS 'FALSE oculta da vitrine sem apagar o histórico (RN10).';
COMMENT ON COLUMN cupcake.criado_em IS 'Data/hora do cadastro.';

COMMENT ON TABLE pedido IS 'Pedido de compra de um cliente.';
COMMENT ON COLUMN pedido.id IS 'Número do pedido.';
COMMENT ON COLUMN pedido.usuario_id IS 'Cliente que fez o pedido.';
COMMENT ON COLUMN pedido.data_hora IS 'Data/hora da criação do pedido.';
COMMENT ON COLUMN pedido.status IS 'AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARO, ENVIADO, ENTREGUE ou CANCELADO (RN07).';
COMMENT ON COLUMN pedido.subtotal IS 'Soma dos subtotais dos itens.';
COMMENT ON COLUMN pedido.frete IS 'Frete calculado pela RN02.';
COMMENT ON COLUMN pedido.total IS 'subtotal + frete (garantido por CHECK).';
COMMENT ON COLUMN pedido.endereco_entrega IS 'Endereço de entrega informado no checkout.';

COMMENT ON TABLE item_pedido IS 'Itens de um pedido (composição).';
COMMENT ON COLUMN item_pedido.id IS 'Identificador sequencial.';
COMMENT ON COLUMN item_pedido.pedido_id IS 'Pedido ao qual o item pertence.';
COMMENT ON COLUMN item_pedido.cupcake_id IS 'Cupcake comprado.';
COMMENT ON COLUMN item_pedido.quantidade IS 'Quantidade (1 a 20, RN03).';
COMMENT ON COLUMN item_pedido.preco_unitario IS 'Preço do cupcake congelado no momento da compra (RN04).';
COMMENT ON COLUMN item_pedido.subtotal IS 'preco_unitario * quantidade (garantido por CHECK).';

COMMENT ON TABLE pagamento IS 'Pagamento do pedido (1:1).';
COMMENT ON COLUMN pagamento.id IS 'Identificador sequencial.';
COMMENT ON COLUMN pagamento.pedido_id IS 'Pedido pago (único).';
COMMENT ON COLUMN pagamento.metodo IS 'Forma de pagamento. No MVP apenas PIX.';
COMMENT ON COLUMN pagamento.status IS 'PENDENTE, CONFIRMADO ou CANCELADO (RN09).';
COMMENT ON COLUMN pagamento.valor IS 'Valor cobrado, igual ao total do pedido.';
COMMENT ON COLUMN pagamento.codigo_pix IS 'BR Code PIX copia e cola (simulado).';
COMMENT ON COLUMN pagamento.criado_em IS 'Data/hora da geração da cobrança.';
COMMENT ON COLUMN pagamento.confirmado_em IS 'Data/hora da confirmação do pagamento.';
