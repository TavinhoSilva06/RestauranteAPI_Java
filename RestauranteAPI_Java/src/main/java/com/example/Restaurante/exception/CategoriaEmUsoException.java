package com.example.Restaurante.exception;

public class CategoriaEmUsoException extends RuntimeException {
    public CategoriaEmUsoException(String id) {
        super("Categoria em uso por pratos e não pode ser removida: " + id);
    }
}
