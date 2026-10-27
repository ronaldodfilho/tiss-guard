package br.com.tissguard.validacao;

import java.util.List;
import java.util.Objects;

public record ResultadoValidacao(
        String nomeArquivoXml,
        String versaoEsquema,
        StatusValidacao status,
        List<ErroValidacao> erros,
        String mensagemInformativa
) {
    public ResultadoValidacao {
        Objects.requireNonNull(nomeArquivoXml, "O nome do arquivo XML não pode ser nulo");
        Objects.requireNonNull(versaoEsquema, "A versão do esquema não pode ser nula");
        Objects.requireNonNull(status, "O status de validação não pode ser nulo");
        if (nomeArquivoXml.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo XML não pode estar em branco");
        }
        if (versaoEsquema.isBlank()) {
            throw new IllegalArgumentException("A versão do esquema não pode estar em branco");
        }
        erros = erros == null ? List.of() : List.copyOf(erros);
    }

    public boolean isValido() {
        return status == StatusValidacao.VALIDO;
    }

    public boolean temErroSintaxe() {
        return erros.stream().anyMatch(erro -> erro.categoria() == CategoriaErroValidacao.SINTAXE_XML);
    }

    public boolean temErroEsquema() {
        return erros.stream().anyMatch(erro -> erro.categoria() == CategoriaErroValidacao.ESQUEMA_XSD);
    }
}
