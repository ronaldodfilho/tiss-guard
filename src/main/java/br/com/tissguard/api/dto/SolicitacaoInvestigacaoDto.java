package br.com.tissguard.api.dto;

import br.com.tissguard.agente.FerramentaConsultaArtefatos;
import br.com.tissguard.agente.TipoPerguntaInvestigacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitacaoInvestigacaoDto(
        @NotNull(message = "O tipo da pergunta é obrigatório")
        TipoPerguntaInvestigacao tipo,

        @NotBlank(message = "O texto da pergunta não pode estar vazio")
        String textoPergunta,

        SolicitacaoValidacaoXmlDto dadosValidacao,

        SolicitacaoComparacaoEsquemasDto dadosComparacao,

        FerramentaConsultaArtefatos.Parametros dadosConsulta
) {
}
