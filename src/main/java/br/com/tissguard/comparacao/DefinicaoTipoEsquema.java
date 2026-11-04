package br.com.tissguard.comparacao;

import java.util.Objects;

public record DefinicaoTipoEsquema(
        String nome,
        String categoria,
        String tipoBase
) {
    public DefinicaoTipoEsquema {
        Objects.requireNonNull(nome, "O nome do tipo não pode ser nulo");
        Objects.requireNonNull(categoria, "A categoria do tipo não pode ser nula");

        if (nome.isBlank()) {
            throw new IllegalArgumentException("O nome do tipo não pode estar em branco");
        }
        if (categoria.isBlank()) {
            throw new IllegalArgumentException("A categoria do tipo não pode estar em branco");
        }
        if (tipoBase == null) {
            tipoBase = "";
        }
    }
}
