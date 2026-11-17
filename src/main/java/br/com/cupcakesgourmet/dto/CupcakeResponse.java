package br.com.cupcakesgourmet.dto;

import br.com.cupcakesgourmet.model.Cupcake;

import java.math.BigDecimal;

public record CupcakeResponse(Long id, String sabor, String descricao, BigDecimal preco,
                              Integer estoque, String imagemUrl, boolean ativo) {

    public static CupcakeResponse de(Cupcake c) {
        return new CupcakeResponse(c.getId(), c.getSabor(), c.getDescricao(), c.getPreco(),
                c.getEstoque(), c.getImagemUrl(), c.isAtivo());
    }
}
