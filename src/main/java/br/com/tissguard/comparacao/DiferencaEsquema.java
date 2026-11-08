package br.com.tissguard.comparacao;

import java.util.Objects;

public record DiferencaEsquema(
        TipoDiferencaEsquema tipo,
        SeveridadeDiferenca severidade,
        String caminhoEstrutural,
        String descricao,
        String valorAnterior,
        String valorNovo
) {
    public DiferencaEsquema {
        Objects.requireNonNull(tipo, "O tipo da diferença não pode ser nulo");
        Objects.requireNonNull(severidade, "A severidade da diferença não pode ser nula");
        Objects.requireNonNull(caminhoEstrutural, "O caminho estrutural não pode ser nulo");
        Objects.requireNonNull(descricao, "A descrição da diferença não pode ser nula");

        if (caminhoEstrutural.isBlank()) {
            throw new IllegalArgumentException("O caminho estrutural não pode estar em branco");
        }
        if (descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição da diferença não pode estar em branco");
        }
    }

    public DiferencaEsquema(
            TipoDiferencaEsquema tipo,
            String caminhoEstrutural,
            String descricao,
            String valorAnterior,
            String valorNovo
    ) {
        this(tipo, SeveridadeDiferenca.RESTRITIVA, caminhoEstrutural, descricao, valorAnterior, valorNovo);
    }

    public boolean isQuebraDeContrato() {
        return severidade == SeveridadeDiferenca.QUEBRA_DE_CONTRATO;
    }

    public boolean isAditiva() {
        return severidade == SeveridadeDiferenca.ADITIVA;
    }
}
