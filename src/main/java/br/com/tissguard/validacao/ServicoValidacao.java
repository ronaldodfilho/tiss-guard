package br.com.tissguard.validacao;

import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import org.springframework.stereotype.Service;

import javax.xml.transform.Source;
import java.util.Objects;

@Service
public class ServicoValidacao {

    private final ValidadorXml validadorXml;
    private final LocalizadorEsquemas localizadorEsquemas;

    public ServicoValidacao(ValidadorXml validadorXml, LocalizadorEsquemas localizadorEsquemas) {
        this.validadorXml = Objects.requireNonNull(validadorXml, "O validador XML não pode ser nulo");
        this.localizadorEsquemas = Objects.requireNonNull(localizadorEsquemas, "O localizador de esquemas não pode ser nulo");
    }

    public ResultadoValidacao validar(SolicitacaoValidacaoXmlDto solicitacao) {
        Objects.requireNonNull(solicitacao, "A solicitação de validação não pode ser nula");

        Source fonteEsquema = localizadorEsquemas.localizarFonte(
                solicitacao.versaoEsquema(),
                null
        );

        return validadorXml.validar(
                solicitacao.nomeArquivo(),
                solicitacao.conteudoXml(),
                solicitacao.versaoEsquema(),
                fonteEsquema
        );
    }
}
