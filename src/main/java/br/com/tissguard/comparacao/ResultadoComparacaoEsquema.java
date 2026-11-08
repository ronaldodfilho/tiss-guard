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
        DiagnosticoCompatibilidade diagnostico,
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
        Objects.requireNonNull(diagnostico, "O diagnóstico de compatibilidade não pode ser nulo");

        diferencas = Collections.unmodifiableList(diferencas);
    }

    public ResultadoComparacaoEsquema(
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
        this(
                identificadorEsquemaBase,
                versaoEsquemaBase,
                hashEsquemaBase,
                identificadorEsquemaComparado,
                versaoEsquemaComparado,
                hashEsquemaComparado,
                diferencas,
                DiagnosticoCompatibilidade.gerarPara(diferencas),
                esquemasIdenticos,
                totalDiferencas
        );
    }

    public List<DiferencaEsquema> diferencasPorTipo(TipoDiferencaEsquema tipo) {
        if (tipo == null) {
            return Collections.emptyList();
        }
        return diferencas.stream()
                .filter(d -> d.tipo() == tipo)
                .toList();
    }

    public List<DiferencaEsquema> quebrasDeContrato() {
        return diferencas.stream()
                .filter(DiferencaEsquema::isQuebraDeContrato)
                .toList();
    }

    public List<DiferencaEsquema> alteracoesAditivas() {
        return diferencas.stream()
                .filter(DiferencaEsquema::isAditiva)
                .toList();
    }

    public boolean isCompativelComVersaoAnterior() {
        return diagnostico.compativelComVersaoAnterior();
    }
}
