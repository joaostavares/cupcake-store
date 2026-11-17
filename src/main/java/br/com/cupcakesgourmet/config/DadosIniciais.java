package br.com.cupcakesgourmet.config;

import br.com.cupcakesgourmet.model.Papel;
import br.com.cupcakesgourmet.repository.UsuarioRepository;
import br.com.cupcakesgourmet.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador (credenciais via variáveis ADMIN_EMAIL / ADMIN_PASSWORD) e um cliente de demonstração,
 * caso ainda não existam. Fica fora das migrations para que a senha não fique fixa no repositório.
 */
@Component
class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final AppProperties props;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    DadosIniciais(AppProperties props, UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.props = props;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String adminEmail = UsuarioService.normalizarEmail(props.admin().email());
        if (!usuarioRepository.existsByEmail(adminEmail)) {
            usuarioService.criar("Administrador", adminEmail, props.admin().senha(), null, null, Papel.ADMIN);
            log.info("Administrador criado: {}", adminEmail);
        }
        if (props.demo().criarCliente()) {
            String email = UsuarioService.normalizarEmail(props.demo().email());
            if (!usuarioRepository.existsByEmail(email)) {
                usuarioService.criar("Cliente Demonstração", email, props.demo().senha(), "(11) 99999-0000",
                        "Rua das Flores, 123 - Centro - São Paulo/SP", Papel.CLIENTE);
                log.info("Cliente de demonstração criado: {}", email);
            }
        }
    }
}
