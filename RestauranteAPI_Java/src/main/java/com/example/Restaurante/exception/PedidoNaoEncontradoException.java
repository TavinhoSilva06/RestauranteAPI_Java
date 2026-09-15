package com.example.Restaurante.exception;

public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(String id) {
        super("Pedido não encontrado: " + id);
    }
}
