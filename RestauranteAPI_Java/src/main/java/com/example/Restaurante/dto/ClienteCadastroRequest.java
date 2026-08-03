package com.example.Restaurante.dto;

public record ClienteCadastroRequest(
        String nome,
        String email,
        String senha
) {
}
