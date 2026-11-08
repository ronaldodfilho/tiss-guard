package br.com.tissguard.api.dto;

import br.com.tissguard.comparacao.DiferencaEsquema;
import br.com.tissguard.comparacao.SeveridadeDiferenca;
import br.com.tissguard.comparacao.TipoDiferencaEsquema;

import java.util.Objects;

public record DiferencaEsquemaItemDto(
        TipoDiferencaEsquema tipo,
        SeveridadeDiferenca severidade,
        String caminhoEstrutural,
        String descricao,
        String valorAnterior,
        String valorNovo
) {
    public DiferencaEsquemaItemDto {
        Objects.requireNonNull(tipo, "O tipo da diferença não pode ser nulo");
        Objects.requireNonNull(severidade, "A severidade da diferença não pode ser nula");
        Objects.requireNonNull(caminhoEstrutural, "O caminho estrutural não pode ser nulo");
        Objects.requireNonNull(descricao, "A descrição não pode ser nula");
    }

    public static DiferencaEsquemaItemDto de(DiferencaEsquema diferenca) {
        Objects.requireNonNull(diferenca, "A diferença não pode ser nula");
        return new DiferencaEsquemaItemDto(
                diferenca.tipo(),
                diferenca.severidade(),
                diferenca.caminhoEstrutural(),
                diferenca.descricao(),
                diferenca.valorAnterior(),
                diferenca.valorNovo()
        );
    }
}
