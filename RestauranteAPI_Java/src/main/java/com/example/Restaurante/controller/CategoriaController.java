package com.example.Restaurante.controller;

import com.example.Restaurante.dto.CategoriaRequest;
import com.example.Restaurante.dto.CategoriaResponse;
import com.example.Restaurante.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        List<CategoriaResponse> categorias = categoriaService.listarTodas();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscar(@PathVariable String id) {
        CategoriaResponse categoria = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(categoria);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoria = categoriaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> editar(@PathVariable String id, @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoria = categoriaService.editar(id, request);
        return ResponseEntity.ok(categoria);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remover(@PathVariable String id) {
        categoriaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
