package com.example.Restaurante.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PedidoItemRequest(
        @NotBlank(message = "Prato é obrigatório") String pratoId,
        @NotNull(message = "Quantidade é obrigatória") @Min(value = 1, message = "Quantidade deve ser maior que zero") Integer quantidade
) {}
