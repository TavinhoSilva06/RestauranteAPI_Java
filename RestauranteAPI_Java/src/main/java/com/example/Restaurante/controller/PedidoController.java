package com.example.Restaurante.controller;

import com.example.Restaurante.dto.PedidoRequest;
import com.example.Restaurante.dto.PedidoResponse;
import com.example.Restaurante.dto.PedidoStatusUpdateRequest;
import com.example.Restaurante.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request, Authentication authentication) {
        PedidoResponse pedido = pedidoService.criar(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<List<PedidoResponse>> listarTodos() {
        List<PedidoResponse> pedidos = pedidoService.listarTodos();
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PedidoResponse>> listarMeus(Authentication authentication) {
        List<PedidoResponse> pedidos = pedidoService.listarMeusPedidos(authentication);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable String id, Authentication authentication) {
        PedidoResponse pedido = pedidoService.buscarPorId(id, authentication);
        return ResponseEntity.ok(pedido);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FUNCIONARIO','ADMIN')")
    public ResponseEntity<PedidoResponse> atualizarStatus(@PathVariable String id, @Valid @RequestBody PedidoStatusUpdateRequest request) {
        PedidoResponse pedido = pedidoService.atualizarStatus(id, request);
        return ResponseEntity.ok(pedido);
    }
}
