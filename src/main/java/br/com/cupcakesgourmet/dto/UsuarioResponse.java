package br.com.cupcakesgourmet.dto;

import br.com.cupcakesgourmet.model.Papel;
import br.com.cupcakesgourmet.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String telefone, String endereco, Papel papel) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getEndereco(), u.getPapel());
    }
}
