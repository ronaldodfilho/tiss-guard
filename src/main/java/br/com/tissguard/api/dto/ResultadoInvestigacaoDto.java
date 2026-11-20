package br.com.tissguard.api.dto;

import br.com.tissguard.agente.RespostaInvestigacao;

public record ResultadoInvestigacaoDto(
        String ferramentaUtilizada,
        Object resultadoTecnico,
        String explicacaoFundamentada,
        boolean evidenciaSuficiente,
        String limitacaoOuAlerta
) {
    public static ResultadoInvestigacaoDto de(RespostaInvestigacao resposta) {
        return new ResultadoInvestigacaoDto(
                resposta.ferramentaUtilizada(),
                resposta.resultadoTecnico(),
                resposta.explicacaoFundamentada(),
                resposta.evidenciaSuficiente(),
                resposta.limitacaoOuAlerta()
        );
    }
}
