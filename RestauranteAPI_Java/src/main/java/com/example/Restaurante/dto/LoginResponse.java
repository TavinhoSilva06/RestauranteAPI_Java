package com.example.Restaurante.dto;

public record LoginResponse(
        String token,
        ClienteResponse cliente
) {
}
