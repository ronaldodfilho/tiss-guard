package br.com.tissguard.agente;

import java.util.Objects;

public record RespostaInvestigacao(
        String ferramentaUtilizada,
        Object resultadoTecnico,
        String explicacaoFundamentada,
        boolean evidenciaSuficiente,
        String limitacaoOuAlerta
) {
    public RespostaInvestigacao {
        Objects.requireNonNull(explicacaoFundamentada, "A explicação fundamentada não pode ser nula");
    }

    public static RespostaInvestigacao comSucesso(String ferramenta, Object resultadoTecnico, String explicacao) {
        return new RespostaInvestigacao(
                ferramenta,
                resultadoTecnico,
                explicacao,
                true,
                "Análise técnica determinística concluída com evidências reproduzíveis."
        );
    }

    public static RespostaInvestigacao semEvidencia(String explicacao, String limitacao) {
        return new RespostaInvestigacao(
                "nenhuma",
                null,
                explicacao,
                false,
                limitacao
        );
    }
}

