package br.com.tissguard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitacaoValidacaoXmlDto(
        @NotBlank(message = "O nome do arquivo é obrigatório")
        String nomeArquivo,

        @NotBlank(message = "A versão do esquema é obrigatória")
        String versaoEsquema,

        @NotBlank(message = "O conteúdo XML é obrigatório")
        String conteudoXml
) {
}
