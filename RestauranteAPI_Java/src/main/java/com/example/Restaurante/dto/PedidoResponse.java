package com.example.Restaurante.dto;

import com.example.Restaurante.document.PedidoStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
        String id,
        String clienteId,
        List<PedidoItemResponse> itens,
        BigDecimal valorTotal,
        PedidoStatus status,
        Instant dataCriacao,
        Instant dataAtualizacao
) {}
