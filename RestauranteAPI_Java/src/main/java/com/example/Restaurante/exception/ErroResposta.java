package com.example.Restaurante.exception;

import java.time.Instant;

public record ErroResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem
) {
}
