package br.com.tissguard.api.dto;

import br.com.tissguard.comparacao.ResultadoComparacaoEsquema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ResultadoComparacaoEsquemasDto(
        String identificadorEsquemaBase,
        String versaoEsquemaBase,
        String hashEsquemaBase,
        String identificadorEsquemaComparado,
        String versaoEsquemaComparado,
        String hashEsquemaComparado,
        boolean esquemasIdenticos,
        boolean compativelRetroativamente,
        int totalQuebrasContrato,
        int totalAlteracoesAditivas,
        int totalAlteracoesRestritivas,
        String resumoDiagnostico,
        String recomendacaoIntegracao,
        List<DiferencaEsquemaItemDto> diferencas,
        int totalDiferencas
) {
    public ResultadoComparacaoEsquemasDto {
        Objects.requireNonNull(identificadorEsquemaBase, "O identificador base não pode ser nulo");
        Objects.requireNonNull(versaoEsquemaBase, "A versão base não pode ser nula");
        Objects.requireNonNull(hashEsquemaBase, "O hash base não pode ser nulo");
        Objects.requireNonNull(identificadorEsquemaComparado, "O identificador comparado não pode ser nulo");
        Objects.requireNonNull(versaoEsquemaComparado, "A versão comparada não pode ser nula");
        Objects.requireNonNull(hashEsquemaComparado, "O hash comparado não pode ser nulo");
        Objects.requireNonNull(resumoDiagnostico, "O resumo do diagnóstico não pode ser nulo");
        Objects.requireNonNull(recomendacaoIntegracao, "A recomendação não pode ser nula");
        if (diferencas == null) {
            diferencas = Collections.emptyList();
        } else {
            diferencas = Collections.unmodifiableList(diferencas);
        }
    }

    public static ResultadoComparacaoEsquemasDto de(ResultadoComparacaoEsquema resultado) {
        Objects.requireNonNull(resultado, "O resultado da comparação não pode ser nulo");

        List<DiferencaEsquemaItemDto> lista = resultado.diferencas().stream()
                .map(DiferencaEsquemaItemDto::de)
                .toList();

        return new ResultadoComparacaoEsquemasDto(
                resultado.identificadorEsquemaBase(),
                resultado.versaoEsquemaBase(),
                resultado.hashEsquemaBase(),
                resultado.identificadorEsquemaComparado(),
                resultado.versaoEsquemaComparado(),
                resultado.hashEsquemaComparado(),
                resultado.esquemasIdenticos(),
                resultado.isCompativelComVersaoAnterior(),
                resultado.diagnostico().totalQuebrasContrato(),
                resultado.diagnostico().totalAlteracoesAditivas(),
                resultado.diagnostico().totalAlteracoesRestritivas(),
                resultado.diagnostico().resumoDiagnostico(),
                resultado.diagnostico().recomendacaoIntegracao(),
                lista,
                resultado.totalDiferencas()
        );
    }
}
