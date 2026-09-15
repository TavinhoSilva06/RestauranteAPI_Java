package com.example.Restaurante.dto;

import java.math.BigDecimal;

public record PedidoItemResponse(
        String pratoId,
        String nomePrato,
        BigDecimal precoUnitario,
        int quantidade,
        BigDecimal subtotal
) {}
