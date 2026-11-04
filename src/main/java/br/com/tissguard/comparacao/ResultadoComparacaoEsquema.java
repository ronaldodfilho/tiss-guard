package br.com.tissguard.comparacao;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ResultadoComparacaoEsquema(
        String identificadorEsquemaBase,
        String versaoEsquemaBase,
        String hashEsquemaBase,
        String identificadorEsquemaComparado,
        String versaoEsquemaComparado,
        String hashEsquemaComparado,
        List<DiferencaEsquema> diferencas,
        boolean esquemasIdenticos,
        int totalDiferencas
) {
    public ResultadoComparacaoEsquema {
        Objects.requireNonNull(identificadorEsquemaBase, "O identificador do esquema base não pode ser nulo");
        Objects.requireNonNull(versaoEsquemaBase, "A versão do esquema base não pode ser nula");
        Objects.requireNonNull(hashEsquemaBase, "O hash do esquema base não pode ser nulo");
        Objects.requireNonNull(identificadorEsquemaComparado, "O identificador do esquema comparado não pode ser nulo");
        Objects.requireNonNull(versaoEsquemaComparado, "A versão do esquema comparado não pode ser nula");
        Objects.requireNonNull(hashEsquemaComparado, "O hash do esquema comparado não pode ser nulo");
        Objects.requireNonNull(diferencas, "A lista de diferenças não pode ser nula");

        diferencas = Collections.unmodifiableList(diferencas);
    }

    public List<DiferencaEsquema> diferencasPorTipo(TipoDiferencaEsquema tipo) {
        if (tipo == null) {
            return Collections.emptyList();
        }
        return diferencas.stream()
                .filter(d -> d.tipo() == tipo)
                .toList();
    }
}
