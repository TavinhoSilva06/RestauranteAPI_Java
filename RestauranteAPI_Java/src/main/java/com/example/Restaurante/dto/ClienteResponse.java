package com.example.Restaurante.dto;

import com.example.Restaurante.document.Papel;

public record ClienteResponse(
        String id,
        String nome,
        String email,
        Papel papel
) {
}
