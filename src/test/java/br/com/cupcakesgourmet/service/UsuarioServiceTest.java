package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.dto.CadastroRequest;
import br.com.cupcakesgourmet.dto.UsuarioResponse;
import br.com.cupcakesgourmet.exception.ConflitoException;
import br.com.cupcakesgourmet.model.Papel;
import br.com.cupcakesgourmet.model.Usuario;
import br.com.cupcakesgourmet.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RN06 — Cadastro de usuário")
class UsuarioServiceTest {

    @Mock
    UsuarioRepository repository;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void cadastraComEmailNormalizadoESenhaCriptografada() {
        UsuarioService service = new UsuarioService(repository, encoder);
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = service.cadastrar(new CadastroRequest(" Maria ", "  Maria@Exemplo.COM ", "segredo1", "", "Rua A"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(salvo.getNome()).isEqualTo("Maria");
        assertThat(salvo.getSenhaHash()).isNotEqualTo("segredo1").startsWith("$2");
        assertThat(encoder.matches("segredo1", salvo.getSenhaHash())).isTrue();
        assertThat(salvo.getTelefone()).isNull();
        assertThat(salvo.getPapel()).isEqualTo(Papel.CLIENTE);
        assertThat(resp.papel()).isEqualTo(Papel.CLIENTE);
    }

    @Test
    void rejeitaEmailDuplicadoIgnorandoMaiusculas() {
        UsuarioService service = new UsuarioService(repository, encoder);
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new CadastroRequest("Maria", "MARIA@exemplo.com", "segredo1", null, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageStartingWith("Este e-mail já está cadastrado");
        verify(repository, never()).save(any());
    }
}
