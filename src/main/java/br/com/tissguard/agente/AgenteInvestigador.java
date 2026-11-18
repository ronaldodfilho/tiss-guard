package br.com.tissguard.agente;

import br.com.tissguard.api.dto.ArtefatoCatalogoDto;
import br.com.tissguard.api.dto.ErroValidacaoItemDto;
import br.com.tissguard.api.dto.ResultadoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.ResultadoValidacaoXmlDto;
import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class AgenteInvestigador {

    private final FerramentaConsultaArtefatos ferramentaConsulta;
    private final FerramentaValidacaoXml ferramentaValidacao;
    private final FerramentaComparacaoEsquemas ferramentaComparacao;

    public AgenteInvestigador(
            FerramentaConsultaArtefatos ferramentaConsulta,
            FerramentaValidacaoXml ferramentaValidacao,
            FerramentaComparacaoEsquemas ferramentaComparacao
    ) {
        this.ferramentaConsulta = Objects.requireNonNull(ferramentaConsulta, "A ferramenta de consulta não pode ser nula");
        this.ferramentaValidacao = Objects.requireNonNull(ferramentaValidacao, "A ferramenta de validação não pode ser nula");
        this.ferramentaComparacao = Objects.requireNonNull(ferramentaComparacao, "A ferramenta de comparação não pode ser nula");
    }

    public List<String> obterFerramentasDisponiveis() {
        return List.of(
                ferramentaConsulta.nome(),
                ferramentaValidacao.nome(),
                ferramentaComparacao.nome()
        );
    }

    public boolean ehFerramentaPermitida(String nomeFerramenta) {
        if (nomeFerramenta == null || nomeFerramenta.isBlank()) {
            return false;
        }
        return obterFerramentasDisponiveis().contains(nomeFerramenta);
    }

    public RespostaInvestigacao executarFerramentaPorNome(String nomeFerramenta, Object entrada) {
        if (!ehFerramentaPermitida(nomeFerramenta)) {
            return RespostaInvestigacao.semEvidencia(
                    "Acesso negado: a ferramenta '" + nomeFerramenta + "' não é permitida ou não existe no ambiente seguro do agente.",
                    "O agente possui limites rígidos de segurança. Ferramentas autorizadas: " + obterFerramentasDisponiveis()
            );
        }

        if (ferramentaConsulta.nome().equals(nomeFerramenta)) {
            FerramentaConsultaArtefatos.Parametros parametros = entrada instanceof FerramentaConsultaArtefatos.Parametros p ? p : null;
            return investigarConsulta(PerguntaInvestigacao.paraConsulta("Consulta direta de artefato", parametros));
        }

        if (ferramentaValidacao.nome().equals(nomeFerramenta)) {
            if (entrada instanceof SolicitacaoValidacaoXmlDto solicitacao) {
                return investigarValidacao(PerguntaInvestigacao.paraValidacao("Validação direta de XML", solicitacao));
            }
            return RespostaInvestigacao.semEvidencia(
                    "Evidência insuficiente: parâmetros inválidos para validação XML.",
                    "Forneça um objeto SolicitacaoValidacaoXmlDto válido."
            );
        }

        if (ferramentaComparacao.nome().equals(nomeFerramenta)) {
            if (entrada instanceof SolicitacaoComparacaoEsquemasDto solicitacao) {
                return investigarComparacao(PerguntaInvestigacao.paraComparacao("Comparação direta de esquemas", solicitacao));
            }
            return RespostaInvestigacao.semEvidencia(
                    "Evidência insuficiente: parâmetros inválidos para comparação de esquemas.",
                    "Forneça um objeto SolicitacaoComparacaoEsquemasDto válido."
            );
        }

        return RespostaInvestigacao.semEvidencia(
                "Ferramenta não executável no fluxo atual.",
                "Consulte as ferramentas homologadas."
        );
    }

    public RespostaInvestigacao investigar(PerguntaInvestigacao pergunta) {
        Objects.requireNonNull(pergunta, "A pergunta de investigação não pode ser nula");

        return switch (pergunta.tipo()) {
            case VALIDACAO_XML -> investigarValidacao(pergunta);
            case COMPARACAO_ESQUEMAS -> investigarComparacao(pergunta);
            case CONSULTA_ARTEFATO -> investigarConsulta(pergunta);
            case FORA_DE_ESCOPO -> RespostaInvestigacao.semEvidencia(
                    "Solicitação fora do escopo técnico do TISS Guard. O agente opera estritamente sobre artefatos oficiais, validação contra esquemas XSD e comparação estrutural.",
                    "Não é permitido processamento de dados reais de pacientes, execução arbitrária de código ou especulação sem ferramenta técnica."
            );
        };
    }

    private RespostaInvestigacao investigarValidacao(PerguntaInvestigacao pergunta) {
        if (pergunta.dadosValidacao() == null) {
            return RespostaInvestigacao.semEvidencia(
                    "Evidência insuficiente: os dados do XML e a versão do esquema não foram fornecidos.",
                    "Forneça o nome do arquivo, conteúdo XML sintético e a versão alvo para investigação técnica."
            );
        }

        ResultadoValidacaoXmlDto resultado = ferramentaValidacao.executar(pergunta.dadosValidacao());

        if (resultado.valido()) {
            String explicacao = "O documento XML '" + resultado.nomeArquivoXml() + "' é válido e está em total conformidade estrutural com o esquema XSD da versão " + resultado.versaoEsquema() + ". Nenhuma inconsistência técnica foi encontrada.";
            return RespostaInvestigacao.comSucesso(ferramentaValidacao.nome(), resultado, explicacao);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("O documento XML '").append(resultado.nomeArquivoXml())
                .append("' falhou na validação contra o esquema XSD da versão ")
                .append(resultado.versaoEsquema())
                .append(". Foram identificadas ").append(resultado.totalErros()).append(" inconsistência(s) técnica(s): ");

        for (int i = 0; i < resultado.erros().size(); i++) {
            ErroValidacaoItemDto erro = resultado.erros().get(i);
            sb.append("[").append(i + 1).append("] Linha ").append(erro.linha())
                    .append(", Coluna ").append(erro.coluna())
                    .append(" (").append(erro.categoria()).append("): ")
                    .append(erro.mensagem()).append("; ");
        }

        return RespostaInvestigacao.comSucesso(ferramentaValidacao.nome(), resultado, sb.toString().trim());
    }

    private RespostaInvestigacao investigarComparacao(PerguntaInvestigacao pergunta) {
        if (pergunta.dadosComparacao() == null) {
            return RespostaInvestigacao.semEvidencia(
                    "Evidência insuficiente: as versões para comparação de esquemas não foram informadas.",
                    "Indique a versão base e a versão comparada para análise estrutural de compatibilidade."
            );
        }

        ResultadoComparacaoEsquemasDto resultado = ferramentaComparacao.executar(pergunta.dadosComparacao());

        StringBuilder sb = new StringBuilder();
        sb.append("Comparação estrutural entre a versão base ").append(resultado.versaoEsquemaBase())
                .append(" (hash SHA-256: ").append(resultado.hashEsquemaBase()).append(") e a versão comparada ")
                .append(resultado.versaoEsquemaComparado())
                .append(" (hash SHA-256: ").append(resultado.hashEsquemaComparado()).append("). ");

        if (resultado.esquemasIdenticos()) {
            sb.append("Os esquemas são totalmente idênticos.");
        } else {
            sb.append("Resultado: ").append(resultado.totalDiferencas()).append(" diferença(s) detectada(s). ")
                    .append("Diagnóstico: ").append(resultado.resumoDiagnostico()).append(" ")
                    .append("Recomendação técnica: ").append(resultado.recomendacaoIntegracao());
        }

        return RespostaInvestigacao.comSucesso(ferramentaComparacao.nome(), resultado, sb.toString());
    }

    private RespostaInvestigacao investigarConsulta(PerguntaInvestigacao pergunta) {
        List<ArtefatoCatalogoDto> artefatos = ferramentaConsulta.executar(pergunta.dadosConsulta());

        if (artefatos.isEmpty()) {
            return RespostaInvestigacao.semEvidencia(
                    "Nenhum artefato oficial encontrado no catálogo para os parâmetros fornecidos.",
                    "Consulte o catálogo geral ou verifique se a versão informada já foi catalogada."
            );
        }

        StringBuilder sbFinal = new StringBuilder();
        sbFinal.append("Foram localizados ").append(artefatos.size()).append(" artefato(s) oficial(is) no catálogo: ");
        for (ArtefatoCatalogoDto art : artefatos) {
            sbFinal.append("Artefato '").append(art.nome()).append("' (versão ").append(art.versaoDeclarada())
                    .append(", componente ").append(art.componente())
                    .append(", hash: ").append(art.hashSha256()).append("); ");
        }

        return RespostaInvestigacao.comSucesso(ferramentaConsulta.nome(), artefatos, sbFinal.toString().trim());
    }
}
