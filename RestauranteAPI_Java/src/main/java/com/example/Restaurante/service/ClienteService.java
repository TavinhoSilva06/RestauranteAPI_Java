package com.example.Restaurante.service;

import com.example.Restaurante.document.Cliente;
import com.example.Restaurante.document.Papel;
import com.example.Restaurante.dto.ClienteCadastroRequest;
import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.exception.EmailJaCadastradoException;
import com.example.Restaurante.exception.ValidacaoException;
import com.example.Restaurante.repository.ClienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public ClienteService(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ClienteResponse cadastrar(ClienteCadastroRequest request) {
        validar(request);

        if (clienteRepository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException(request.email());
        }

        Cliente cliente = Cliente.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .papel(Papel.CLIENTE)
                .dataCriacao(Instant.now())
                .build();

        Cliente salvo = clienteRepository.save(cliente);

        return new ClienteResponse(salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getPapel());
    }

    private void validar(ClienteCadastroRequest request) {
        if (request.nome() == null || request.nome().isBlank()) {
            throw new ValidacaoException("Nome é obrigatório");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidacaoException("E-mail é obrigatório");
        }
        if (!EMAIL_PATTERN.matcher(request.email()).matches()) {
            throw new ValidacaoException("E-mail inválido");
        }
        if (request.senha() == null || request.senha().isBlank()) {
            throw new ValidacaoException("Senha é obrigatória");
        }
        if (request.senha().length() < 8) {
            throw new ValidacaoException("Senha deve ter no mínimo 8 caracteres");
        }
    }
}
