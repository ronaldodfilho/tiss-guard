package br.com.tissguard.api.dto;

import br.com.tissguard.validacao.ResultadoValidacao;
import br.com.tissguard.validacao.StatusValidacao;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ResultadoValidacaoXmlDto(
        String nomeArquivoXml,
        String versaoEsquema,
        StatusValidacao status,
        boolean valido,
        String mensagemInformativa,
        List<ErroValidacaoItemDto> erros,
        int totalErros
) {
    public ResultadoValidacaoXmlDto {
        Objects.requireNonNull(nomeArquivoXml, "O nome do arquivo XML não pode ser nulo");
        Objects.requireNonNull(versaoEsquema, "A versão do esquema não pode ser nula");
        Objects.requireNonNull(status, "O status da validação não pode ser nulo");
        if (erros == null) {
            erros = Collections.emptyList();
        } else {
            erros = Collections.unmodifiableList(erros);
        }
    }

    public static ResultadoValidacaoXmlDto de(ResultadoValidacao resultado) {
        Objects.requireNonNull(resultado, "O resultado da validação não pode ser nulo");
        List<ErroValidacaoItemDto> listaErros = resultado.erros().stream()
                .map(ErroValidacaoItemDto::de)
                .toList();

        return new ResultadoValidacaoXmlDto(
                resultado.nomeArquivoXml(),
                resultado.versaoEsquema(),
                resultado.status(),
                resultado.isValido(),
                resultado.mensagemInformativa(),
                listaErros,
                resultado.erros().size()
        );
    }
}
