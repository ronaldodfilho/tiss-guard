package br.com.tissguard.comparacao;

import java.util.Objects;

public record DefinicaoElementoEsquema(
        String caminho,
        String nome,
        String tipo,
        String ocorrenciaMinima,
        String ocorrenciaMaxima
) {
    public DefinicaoElementoEsquema {
        Objects.requireNonNull(caminho, "O caminho do elemento não pode ser nulo");
        Objects.requireNonNull(nome, "O nome do elemento não pode ser nulo");

        if (caminho.isBlank()) {
            throw new IllegalArgumentException("O caminho do elemento não pode estar em branco");
        }
        if (nome.isBlank()) {
            throw new IllegalArgumentException("O nome do elemento não pode estar em branco");
        }
        if (ocorrenciaMinima == null || ocorrenciaMinima.isBlank()) {
            ocorrenciaMinima = "1";
        }
        if (ocorrenciaMaxima == null || ocorrenciaMaxima.isBlank()) {
            ocorrenciaMaxima = "1";
        }
        if (tipo == null) {
            tipo = "";
        }
    }
}
