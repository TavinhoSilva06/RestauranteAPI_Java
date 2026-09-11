package com.example.Restaurante.dto;

import java.math.BigDecimal;

public record PratoResponse(
    String id,
    String nome,
    String descricao,
    BigDecimal preco,
    String categoriaId,
    boolean disponivel,
    String imagemUrl
) {}
