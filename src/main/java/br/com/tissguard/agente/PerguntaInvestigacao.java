package br.com.tissguard.agente;

import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;

import java.util.Objects;

public record PerguntaInvestigacao(
        TipoPerguntaInvestigacao tipo,
        String textoPergunta,
        SolicitacaoValidacaoXmlDto dadosValidacao,
        SolicitacaoComparacaoEsquemasDto dadosComparacao,
        FerramentaConsultaArtefatos.Parametros dadosConsulta
) {
    public PerguntaInvestigacao {
        Objects.requireNonNull(tipo, "O tipo da pergunta não pode ser nulo");
        Objects.requireNonNull(textoPergunta, "O texto da pergunta não pode ser nulo");

        if (textoPergunta.isBlank()) {
            throw new IllegalArgumentException("O texto da pergunta não pode estar em branco");
        }
    }

    public static PerguntaInvestigacao paraValidacao(String texto, SolicitacaoValidacaoXmlDto dados) {
        return new PerguntaInvestigacao(TipoPerguntaInvestigacao.VALIDACAO_XML, texto, dados, null, null);
    }

    public static PerguntaInvestigacao paraComparacao(String texto, SolicitacaoComparacaoEsquemasDto dados) {
        return new PerguntaInvestigacao(TipoPerguntaInvestigacao.COMPARACAO_ESQUEMAS, texto, null, dados, null);
    }

    public static PerguntaInvestigacao paraConsulta(String texto, FerramentaConsultaArtefatos.Parametros dados) {
        return new PerguntaInvestigacao(TipoPerguntaInvestigacao.CONSULTA_ARTEFATO, texto, null, null, dados);
    }

    public static PerguntaInvestigacao foraDeEscopo(String texto) {
        return new PerguntaInvestigacao(TipoPerguntaInvestigacao.FORA_DE_ESCOPO, texto, null, null, null);
    }
}

