package com.example.Restaurante.exception;

public class CategoriaNaoEncontradaException extends RuntimeException {
    public CategoriaNaoEncontradaException(String id) {
        super("Categoria não encontrada: " + id);
    }
}
