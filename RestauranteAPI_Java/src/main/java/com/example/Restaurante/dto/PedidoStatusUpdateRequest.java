package com.example.Restaurante.dto;

import com.example.Restaurante.document.PedidoStatus;
import jakarta.validation.constraints.NotNull;

public record PedidoStatusUpdateRequest(
        @NotNull(message = "Status é obrigatório") PedidoStatus status
) {}
