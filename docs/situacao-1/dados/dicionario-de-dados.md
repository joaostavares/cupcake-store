# Dicionário de Dados

> Gerado automaticamente por `scripts/gerar-dicionario.sh` a partir do banco criado pelas migrations Flyway
> (`src/main/resources/db/migration`). As descrições vêm dos `COMMENT ON` da migration V3.

## `cupcake`

Produtos do catálogo.

| Coluna | Tipo | Nulo | Padrão | Chave | Descrição |
|--------|------|------|--------|-------|-----------|
| id | BIGINT | não | identity | PK | Identificador sequencial. |
| sabor | VARCHAR(80) | não |  | UK | Nome do sabor, único. |
| descricao | VARCHAR(500) | sim |  |  | Descrição exibida na vitrine. |
| preco | NUMERIC(10,2) | não |  |  | Preço unitário atual em reais (> 0). |
| estoque | INTEGER | não | 0 |  | Unidades disponíveis (>= 0). Baixado no checkout (RN05). |
| imagem_url | VARCHAR(255) | sim |  |  | Caminho ou URL da imagem. |
| ativo | BOOLEAN | não | true |  | FALSE oculta da vitrine sem apagar o histórico (RN10). |
| criado_em | TIMESTAMPTZ | não | CURRENT_TIMESTAMP |  | Data/hora do cadastro. |

**Restrições:**

- `ck_cupcake_estoque: CHECK ((estoque >= 0))`
- `ck_cupcake_preco: CHECK ((preco > (0)::numeric))`
- `uk_cupcake_sabor: UNIQUE (sabor)`

## `item_pedido`

Itens de um pedido (composição).

| Coluna | Tipo | Nulo | Padrão | Chave | Descrição |
|--------|------|------|--------|-------|-----------|
| id | BIGINT | não | identity | PK | Identificador sequencial. |
| pedido_id | BIGINT | não |  | FK, UK | Pedido ao qual o item pertence. |
| cupcake_id | BIGINT | não |  | FK, UK | Cupcake comprado. |
| quantidade | INTEGER | não |  |  | Quantidade (1 a 20, RN03). |
| preco_unitario | NUMERIC(10,2) | não |  |  | Preço do cupcake congelado no momento da compra (RN04). |
| subtotal | NUMERIC(10,2) | não |  |  | preco_unitario * quantidade (garantido por CHECK). |

**Restrições:**

- `ck_item_quantidade: CHECK ((quantidade > 0))`
- `ck_item_subtotal: CHECK ((subtotal = (preco_unitario * (quantidade)::numeric)))`
- `fk_item_cupcake: FOREIGN KEY (cupcake_id) REFERENCES cupcake(id)`
- `fk_item_pedido: FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE`
- `uk_item_pedido_cupcake: UNIQUE (pedido_id, cupcake_id)`

## `pagamento`

Pagamento do pedido (1:1).

| Coluna | Tipo | Nulo | Padrão | Chave | Descrição |
|--------|------|------|--------|-------|-----------|
| id | BIGINT | não | identity | PK | Identificador sequencial. |
| pedido_id | BIGINT | não |  | FK, UK | Pedido pago (único). |
| metodo | VARCHAR(20) | não |  |  | Forma de pagamento. No MVP apenas PIX. |
| status | VARCHAR(20) | não |  |  | PENDENTE, CONFIRMADO ou CANCELADO (RN09). |
| valor | NUMERIC(10,2) | não |  |  | Valor cobrado, igual ao total do pedido. |
| codigo_pix | VARCHAR(512) | sim |  |  | BR Code PIX copia e cola (simulado). |
| criado_em | TIMESTAMPTZ | não | CURRENT_TIMESTAMP |  | Data/hora da geração da cobrança. |
| confirmado_em | TIMESTAMPTZ | sim |  |  | Data/hora da confirmação do pagamento. |

**Restrições:**

- `ck_pagamento_metodo: CHECK (((metodo)::text = 'PIX'::text))`
- `ck_pagamento_status: CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'CONFIRMADO'::character varying, 'CANCELADO'::character varying])::text[])))`
- `ck_pagamento_valor: CHECK ((valor > (0)::numeric))`
- `fk_pagamento_pedido: FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE`
- `uk_pagamento_pedido: UNIQUE (pedido_id)`

## `pedido`

Pedido de compra de um cliente.

| Coluna | Tipo | Nulo | Padrão | Chave | Descrição |
|--------|------|------|--------|-------|-----------|
| id | BIGINT | não | identity | PK | Número do pedido. |
| usuario_id | BIGINT | não |  | FK | Cliente que fez o pedido. |
| data_hora | TIMESTAMPTZ | não | CURRENT_TIMESTAMP |  | Data/hora da criação do pedido. |
| status | VARCHAR(30) | não |  |  | AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARO, ENVIADO, ENTREGUE ou CANCELADO (RN07). |
| subtotal | NUMERIC(10,2) | não |  |  | Soma dos subtotais dos itens. |
| frete | NUMERIC(10,2) | não |  |  | Frete calculado pela RN02. |
| total | NUMERIC(10,2) | não |  |  | subtotal + frete (garantido por CHECK). |
| endereco_entrega | VARCHAR(255) | não |  |  | Endereço de entrega informado no checkout. |

**Restrições:**

- `ck_pedido_status: CHECK (((status)::text = ANY ((ARRAY['AGUARDANDO_PAGAMENTO'::character varying, 'PAGO'::character varying, 'EM_PREPARO'::character varying, 'ENVIADO'::character varying, 'ENTREGUE'::character varying, 'CANCELADO'::character varying])::text[])))`
- `ck_pedido_valores: CHECK (((subtotal >= (0)::numeric) AND (frete >= (0)::numeric) AND (total = (subtotal + frete))))`
- `fk_pedido_usuario: FOREIGN KEY (usuario_id) REFERENCES usuario(id)`

## `usuario`

Pessoas que acessam o sistema autenticadas: clientes e administradores.

| Coluna | Tipo | Nulo | Padrão | Chave | Descrição |
|--------|------|------|--------|-------|-----------|
| id | BIGINT | não | identity | PK | Identificador sequencial. |
| nome | VARCHAR(100) | não |  |  | Nome completo. |
| email | VARCHAR(150) | não |  | UK | E-mail de login, armazenado em minúsculas, único (RN06). |
| senha_hash | VARCHAR(100) | não |  |  | Hash BCrypt da senha; a senha em texto nunca é gravada. |
| telefone | VARCHAR(20) | sim |  |  | Telefone de contato (opcional). |
| endereco | VARCHAR(255) | sim |  |  | Endereço padrão de entrega (opcional). |
| papel | VARCHAR(20) | não | 'CLIENTE'::character varying |  | CLIENTE ou ADMIN (RN10). |
| criado_em | TIMESTAMPTZ | não | CURRENT_TIMESTAMP |  | Data/hora do cadastro. |

**Restrições:**

- `ck_usuario_papel: CHECK (((papel)::text = ANY ((ARRAY['CLIENTE'::character varying, 'ADMIN'::character varying])::text[])))`
- `uk_usuario_email: UNIQUE (email)`

