package br.com.tissguard.api.dto;

import java.time.Instant;
import java.util.Objects;

public record RespostaPadraoApi<T>(
        boolean sucesso,
        String mensagem,
        T dados,
        Instant timestamp
) {
    public RespostaPadraoApi {
        Objects.requireNonNull(mensagem, "A mensagem não pode ser nula");
        Objects.requireNonNull(timestamp, "O timestamp não pode ser nulo");
    }

    public static <T> RespostaPadraoApi<T> sucesso(T dados, String mensagem) {
        return new RespostaPadraoApi<>(true, mensagem, dados, Instant.now());
    }

    public static <T> RespostaPadraoApi<T> sucesso(T dados) {
        return sucesso(dados, "Operação realizada com sucesso.");
    }
}
