package com.example.Restaurante.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PratoRequest(
    @NotBlank(message = "Nome é obrigatório")
    String nome,

    String descricao,

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Preço deve ser maior que zero")
    BigDecimal preco,

    @NotBlank(message = "Categoria é obrigatória")
    String categoriaId,

    String imagemUrl
) {}
