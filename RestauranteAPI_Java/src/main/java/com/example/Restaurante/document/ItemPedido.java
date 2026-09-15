package com.example.Restaurante.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {
    private String pratoId;
    private String nomePrato;
    private BigDecimal precoUnitario;
    private int quantidade;
    private BigDecimal subtotal;
}
