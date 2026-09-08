package com.example.Restaurante.service;

import com.example.Restaurante.document.Papel;
import com.example.Restaurante.dto.RegistroCadastroRequest;
import com.example.Restaurante.dto.Registro;
import com.example.Restaurante.dto.FuncionarioCadastroRequest;
import com.example.Restaurante.exception.EmailJaCadastradoException;
import com.example.Restaurante.exception.ValidacaoException;
import com.example.Restaurante.repository.RegistroMongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RegistroService {

    private final RegistroMongoTemplate registroMongoTemplate;
    private final PasswordEncoder passwordEncoder;

    public RegistroService(RegistroMongoTemplate registroMongoTemplate, PasswordEncoder passwordEncoder) {
        this.registroMongoTemplate = registroMongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public Registro cadastrar(RegistroCadastroRequest request) {
        return criarCliente(request.nome(), request.email(), request.senha(), Papel.CLIENTE);
    }

    public Registro cadastrarFuncionario(FuncionarioCadastroRequest request) {
        //if (request.papel() == Papel.FUNCIONARIO || request.papel() == Papel.ADMIN) {
        //    return criarCliente(request.nome(), request.email(), request.senha(), Papel.ADMIN);
        //}
        //throw new ValidacaoException("Papel deve ser FUNCIONARIO ou ADMIN");

        if (request.papel() != Papel.FUNCIONARIO && request.papel() != Papel.ADMIN) {
            throw new ValidacaoException("Papel deve ser FUNCIONARIO ou ADMIN");
        }
        return criarCliente(request.nome(), request.email(), request.senha(), request.papel());
    }

    public List<Registro> listarTodos() {
        return registroMongoTemplate.findAllClientes().stream()
                .map(this::toResponse)
                .toList();
    }

    private Registro criarCliente(String nome, String email, String senha, Papel papel) {
        if (registroMongoTemplate.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        com.example.Restaurante.document.Registro registro = com.example.Restaurante.document.Registro.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .papel(papel)
                .dataCriacao(Instant.now())
                .build();

        com.example.Restaurante.document.Registro salvo = registroMongoTemplate.save(registro);
        return toResponse(salvo);
    }

    private Registro toResponse(com.example.Restaurante.document.Registro registro) {
        return new Registro(registro.getId(), registro.getNome(), registro.getEmail(), registro.getPapel());
    }
}
