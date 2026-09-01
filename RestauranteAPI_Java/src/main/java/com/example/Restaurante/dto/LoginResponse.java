package com.example.Restaurante.dto;

public record LoginResponse(
        String token,
        Registro cliente
) {
}
