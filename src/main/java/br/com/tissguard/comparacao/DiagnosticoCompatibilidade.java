package br.com.tissguard.comparacao;

import java.util.Objects;

public record DiagnosticoCompatibilidade(
        boolean compativelComVersaoAnterior,
        int totalQuebrasContrato,
        int totalAlteracoesAditivas,
        int totalAlteracoesRestritivas,
        String resumoDiagnostico,
        String recomendacaoIntegracao
) {
    public DiagnosticoCompatibilidade {
        Objects.requireNonNull(resumoDiagnostico, "O resumo do diagnóstico não pode ser nulo");
        Objects.requireNonNull(recomendacaoIntegracao, "A recomendação de integração não pode ser nula");

        if (resumoDiagnostico.isBlank()) {
            throw new IllegalArgumentException("O resumo do diagnóstico não pode estar em branco");
        }
        if (recomendacaoIntegracao.isBlank()) {
            throw new IllegalArgumentException("A recomendação de integração não pode estar em branco");
        }
    }

    public static DiagnosticoCompatibilidade gerarPara(java.util.List<DiferencaEsquema> diferencas) {
        Objects.requireNonNull(diferencas, "A lista de diferenças não pode ser nula");

        int quebras = (int) diferencas.stream().filter(d -> d.severidade() == SeveridadeDiferenca.QUEBRA_DE_CONTRATO).count();
        int aditivas = (int) diferencas.stream().filter(d -> d.severidade() == SeveridadeDiferenca.ADITIVA).count();
        int restritivas = (int) diferencas.stream().filter(d -> d.severidade() == SeveridadeDiferenca.RESTRITIVA).count();

        boolean compativel = quebras == 0;

        String resumo;
        String recomendacao;

        if (diferencas.isEmpty()) {
            resumo = "Esquemas idênticos. Nenhuma alteração estrutural detectada.";
            recomendacao = "Versões totalmente equivalentes. Nenhuma ação necessária para integração.";
        } else if (compativel) {
            resumo = "Versão compatível retroativamente. Todas as alterações são aditivas ou de relaxamento de regras (" +
                    aditivas + " aditiva(s), " + restritivas + " restritiva(s)).";
            recomendacao = "Integrações legadas continuarão funcionando sem quebra imediata. Novos campos podem ser adotados gradualmente.";
        } else {
            resumo = "Alerta: quebra de contrato detectada. Foram identificadas " + quebras +
                    " alteração(ões) incompatível(is) que impedem retrocompatibilidade total.";
            recomendacao = "Sistemas legados falharão na validação com este esquema caso não sejam adaptados. Exige período de transição ou atualização de clientes antes da migração.";
        }

        return new DiagnosticoCompatibilidade(compativel, quebras, aditivas, restritivas, resumo, recomendacao);
    }
}
