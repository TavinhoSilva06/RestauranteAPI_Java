package com.example.Restaurante.controller;

import com.example.Restaurante.dto.PratoRequest;
import com.example.Restaurante.dto.PratoResponse;
import com.example.Restaurante.service.PratoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pratos")
public class PratoController {

    private final PratoService pratoService;

    public PratoController(PratoService pratoService) {
        this.pratoService = pratoService;
    }

    @GetMapping
    public ResponseEntity<List<PratoResponse>> listar(@RequestParam(required = false) String categoriaId) {
        List<PratoResponse> pratos;
        if (categoriaId != null && !categoriaId.isEmpty()) {
            pratos = pratoService.listarPorCategoria(categoriaId);
        } else {
            pratos = pratoService.listarTodos();
        }
        return ResponseEntity.ok(pratos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PratoResponse> buscar(@PathVariable String id) {
        PratoResponse prato = pratoService.buscarPorId(id);
        return ResponseEntity.ok(prato);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PratoResponse> criar(@Valid @RequestBody PratoRequest request) {
        PratoResponse prato = pratoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(prato);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PratoResponse> editar(@PathVariable String id, @Valid @RequestBody PratoRequest request) {
        PratoResponse prato = pratoService.editar(id, request);
        return ResponseEntity.ok(prato);
    }

    @PatchMapping("/{id}/disponibilidade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PratoResponse> alternarDisponibilidade(@PathVariable String id) {
        PratoResponse prato = pratoService.alternarDisponibilidade(id);
        return ResponseEntity.ok(prato);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remover(@PathVariable String id) {
        pratoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
