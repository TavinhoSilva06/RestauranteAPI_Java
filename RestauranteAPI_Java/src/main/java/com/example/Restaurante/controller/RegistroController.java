package com.example.Restaurante.controller;

import com.example.Restaurante.dto.RegistroCadastroRequest;
import com.example.Restaurante.dto.Registro;
import com.example.Restaurante.dto.FuncionarioCadastroRequest;
import com.example.Restaurante.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping
    public ResponseEntity<Registro> cadastrar(@Valid @RequestBody RegistroCadastroRequest request) {
        Registro response = registroService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN')")
    public ResponseEntity<List<Registro>> listar() {
        List<Registro> response = registroService.listarTodos();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/funcionarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Registro> cadastrarFuncionario(@Valid @RequestBody FuncionarioCadastroRequest request) {
        Registro response = registroService.cadastrarFuncionario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
