package com.example.Restaurante.dto;

import com.example.Restaurante.document.Papel;

public record Registro(
        String id,
        String nome,
        String email,
        Papel papel
) {
}
