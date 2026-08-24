package com.example.Restaurante.service;

import com.example.Restaurante.document.Cliente;
import com.example.Restaurante.document.Papel;
import com.example.Restaurante.dto.ClienteCadastroRequest;
import com.example.Restaurante.dto.ClienteResponse;
import com.example.Restaurante.exception.EmailJaCadastradoException;
import com.example.Restaurante.repository.ClienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ClienteResponse cadastrar(ClienteCadastroRequest request) {
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
}
