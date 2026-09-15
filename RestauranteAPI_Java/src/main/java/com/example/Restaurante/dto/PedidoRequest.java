package com.example.Restaurante.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PedidoRequest(
        @NotEmpty(message = "Pedido deve conter ao menos um item") @Valid List<PedidoItemRequest> itens
) {}
