package com.example.Restaurante.document;

import java.util.Map;
import java.util.Set;

public enum PedidoStatus {
    PENDENTE, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO;

    private static final Map<PedidoStatus, Set<PedidoStatus>> TRANSICOES_VALIDAS = Map.of(
            PENDENTE, Set.of(EM_PREPARO, CANCELADO),
            EM_PREPARO, Set.of(PRONTO, CANCELADO),
            PRONTO, Set.of(ENTREGUE, CANCELADO),
            ENTREGUE, Set.of(),
            CANCELADO, Set.of()
    );

    public boolean podeTransicionarPara(PedidoStatus novoStatus) {
        return TRANSICOES_VALIDAS.get(this).contains(novoStatus);
    }
}
