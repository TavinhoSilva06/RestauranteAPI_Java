package com.example.Restaurante.exception;

import com.example.Restaurante.document.PedidoStatus;

public class TransicaoStatusInvalidaException extends RuntimeException {
    public TransicaoStatusInvalidaException(PedidoStatus statusAtual, PedidoStatus statusNovo) {
        super("Transição de status inválida: " + statusAtual + " -> " + statusNovo);
    }
}
