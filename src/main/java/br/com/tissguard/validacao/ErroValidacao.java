package br.com.tissguard.validacao;

import java.util.Objects;

public record ErroValidacao(
        int linha,
        int coluna,
        String mensagem
) {
    public ErroValidacao {
        Objects.requireNonNull(mensagem, "A mensagem de erro não pode ser nula");
        if (mensagem.isBlank()) {
            throw new IllegalArgumentException("A mensagem de erro não pode estar em branco");
        }
    }
}
