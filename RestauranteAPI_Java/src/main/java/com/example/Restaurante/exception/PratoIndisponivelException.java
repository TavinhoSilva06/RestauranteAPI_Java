package com.example.Restaurante.exception;

public class PratoIndisponivelException extends RuntimeException {
    public PratoIndisponivelException(String pratoId) {
        super("Prato indisponível para pedido: " + pratoId);
    }
}
