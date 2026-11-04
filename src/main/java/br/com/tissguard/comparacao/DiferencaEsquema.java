package br.com.tissguard.comparacao;

import java.util.Objects;

public record DiferencaEsquema(
        TipoDiferencaEsquema tipo,
        String caminhoEstrutural,
        String descricao,
        String valorAnterior,
        String valorNovo
) {
    public DiferencaEsquema {
        Objects.requireNonNull(tipo, "O tipo da diferença não pode ser nulo");
        Objects.requireNonNull(caminhoEstrutural, "O caminho estrutural não pode ser nulo");
        Objects.requireNonNull(descricao, "A descrição da diferença não pode ser nula");

        if (caminhoEstrutural.isBlank()) {
            throw new IllegalArgumentException("O caminho estrutural não pode estar em branco");
        }
        if (descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição da diferença não pode estar em branco");
        }
    }
}
