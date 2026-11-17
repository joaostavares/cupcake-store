package br.com.cupcakesgourmet.service;

import br.com.cupcakesgourmet.dto.CadastroRequest;
import br.com.cupcakesgourmet.dto.UsuarioResponse;
import br.com.cupcakesgourmet.exception.ConflitoException;
import br.com.cupcakesgourmet.exception.RecursoNaoEncontradoException;
import br.com.cupcakesgourmet.model.Papel;
import br.com.cupcakesgourmet.model.Usuario;
import br.com.cupcakesgourmet.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /** RN06 — e-mail único (normalizado em minúsculas) e senha com hash BCrypt. */
    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest req) {
        return UsuarioResponse.de(criar(req.nome(), req.email(), req.senha(), req.telefone(), req.endereco(), Papel.CLIENTE));
    }

    @Transactional
    public Usuario criar(String nome, String email, String senha, String telefone, String endereco, Papel papel) {
        String emailNormalizado = normalizarEmail(email);
        if (repository.existsByEmail(emailNormalizado)) {
            throw new ConflitoException("Este e-mail já está cadastrado. Faça login ou use outro e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(nome.trim());
        u.setEmail(emailNormalizado);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setTelefone(vazioParaNulo(telefone));
        u.setEndereco(vazioParaNulo(endereco));
        u.setPapel(papel);
        return repository.save(u);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return repository.findById(id).map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
