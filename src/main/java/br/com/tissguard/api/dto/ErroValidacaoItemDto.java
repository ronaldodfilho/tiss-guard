package br.com.tissguard.api.dto;

import br.com.tissguard.validacao.CategoriaErroValidacao;
import br.com.tissguard.validacao.ErroValidacao;

import java.util.Objects;

public record ErroValidacaoItemDto(
        int linha,
        int coluna,
        String mensagem,
        CategoriaErroValidacao categoria
) {
    public ErroValidacaoItemDto {
        Objects.requireNonNull(mensagem, "A mensagem de erro não pode ser nula");
        Objects.requireNonNull(categoria, "A categoria do erro não pode ser nula");
    }

    public static ErroValidacaoItemDto de(ErroValidacao erro) {
        Objects.requireNonNull(erro, "O erro de validação não pode ser nulo");
        return new ErroValidacaoItemDto(
                erro.linha(),
                erro.coluna(),
                erro.mensagem(),
                erro.categoria()
        );
    }
}
