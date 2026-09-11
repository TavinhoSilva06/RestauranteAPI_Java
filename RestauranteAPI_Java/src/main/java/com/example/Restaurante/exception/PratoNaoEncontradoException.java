package com.example.Restaurante.exception;

public class PratoNaoEncontradoException extends RuntimeException {
    public PratoNaoEncontradoException(String id) {
        super("Prato não encontrado: " + id);
    }
}
