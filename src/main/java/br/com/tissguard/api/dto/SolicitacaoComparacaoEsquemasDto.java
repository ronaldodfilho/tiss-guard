package br.com.tissguard.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitacaoComparacaoEsquemasDto(
        @NotBlank(message = "A versão base é obrigatória")
        String versaoBase,

        String nomeArquivoBase,

        @NotBlank(message = "A versão comparada é obrigatória")
        String versaoComparada,

        String nomeArquivoComparado
) {
}
