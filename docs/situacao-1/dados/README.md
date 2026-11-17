# Projeto de Dados

| Nível | Artefato |
|-------|----------|
| Conceitual (E-R) | [modelo-conceitual.png](modelo-conceitual.png) · [.puml](modelo-conceitual.puml) |
| Lógico normalizado (3FN) | [modelo-logico.png](modelo-logico.png) · [.puml](modelo-logico.puml) |
| Físico (PostgreSQL 16) | migrations Flyway em [`src/main/resources/db/migration`](../../../src/main/resources/db/migration) |
| Dicionário de dados | [dicionario-de-dados.md](dicionario-de-dados.md) |

## Normalização
- **1FN:** todos os atributos atômicos; o "carrinho" do PIT I virou a tabela `item_pedido` (sem listas dentro do pedido).
- **2FN:** as tabelas usam chave primária simples (`id`), então não há dependência parcial. Em `item_pedido`
  a chave candidata `(pedido_id, cupcake_id)` é garantida por `UNIQUE`.
- **3FN:** não há dependência transitiva entre atributos não-chave. Duas redundâncias são **intencionais e
  controladas**:
  - `item_pedido.preco_unitario` repete `cupcake.preco` *no momento da compra* — é um fato histórico diferente
    (RN04), não uma dependência transitiva.
  - `item_pedido.subtotal` e `pedido.total` são derivados, mantidos por desempenho e auditoria; a consistência
    é garantida por `CHECK` (`subtotal = preco_unitario * quantidade`, `total = subtotal + frete`).

## Decisões do físico
- **SGBD:** PostgreSQL 16 (popular, gratuito, suportado pelo Render).
- Dinheiro em `NUMERIC(10,2)` (nunca ponto flutuante).
- Enums armazenados como `VARCHAR` + `CHECK` (legível em consultas e fácil de evoluir com uma migration).
- Datas em `TIMESTAMP WITH TIME ZONE`.
- `ON DELETE CASCADE` de pedido para itens e pagamento (composição); cupcakes e usuários nunca são apagados
  por cascata.
- O schema é versionado por **Flyway**: `V1__criar_schema.sql` (estrutura), `V2__seed_cupcakes.sql` (catálogo
  de exemplo). O Hibernate roda em modo `validate`, ou seja, o código falha ao subir se divergir do banco.
