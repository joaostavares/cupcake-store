package br.com.cupcakesgourmet.controller;

import br.com.cupcakesgourmet.dto.CadastroRequest;
import br.com.cupcakesgourmet.dto.LoginRequest;
import br.com.cupcakesgourmet.dto.UsuarioResponse;
import br.com.cupcakesgourmet.security.UsuarioLogado;
import br.com.cupcakesgourmet.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU04 — Cadastro e login (logout é tratado pelo Spring Security em POST /api/auth/logout). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UsuarioService usuarioService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest req,
                                                     HttpServletRequest request, HttpServletResponse response) {
        UsuarioResponse usuario = usuarioService.cadastrar(req);
        autenticar(req.email(), req.senha(), request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                                 HttpServletResponse response) {
        UsuarioLogado logado = autenticar(req.email(), req.senha(), request, response);
        return usuarioService.buscar(logado.getId());
    }

    /** 200 com os dados do usuário logado, ou 204 se for visitante. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioLogado usuario) {
        if (usuario == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(usuarioService.buscar(usuario.getId()));
    }

    private UsuarioLogado autenticar(String email, String senha, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(UsuarioService.normalizarEmail(email), senha));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        request.changeSessionId(); // proteção contra fixação de sessão
        securityContextRepository.saveContext(context, request, response);
        return (UsuarioLogado) auth.getPrincipal();
    }
}
