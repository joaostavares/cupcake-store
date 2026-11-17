package br.com.cupcakesgourmet.security;

import br.com.cupcakesgourmet.model.Papel;
import br.com.cupcakesgourmet.model.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Principal guardado na sessão. Expõe id e papel para os controllers/services. */
public class UsuarioLogado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String nome;
    private final String email;
    private final Papel papel;
    private String senhaHash;

    public UsuarioLogado(Long id, String nome, String email, String senhaHash, Papel papel) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
    }

    public static UsuarioLogado de(Usuario u) {
        return new UsuarioLogado(u.getId(), u.getNome(), u.getEmail(), u.getSenhaHash(), u.getPapel());
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Papel getPapel() { return papel; }

    public boolean isAdmin() {
        return papel == Papel.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        senhaHash = null;
    }
}
