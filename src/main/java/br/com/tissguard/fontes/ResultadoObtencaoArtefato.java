package br.com.tissguard.fontes;

import java.util.Objects;

public record ResultadoObtencaoArtefato(
        String nomeArquivo,
        byte[] conteudo,
        int codigoStatusHttp,
        String tipoConteudo,
        long tamanhoBytes
) {
    public ResultadoObtencaoArtefato {
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");
        Objects.requireNonNull(conteudo, "O conteúdo não pode ser nulo");
        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo não pode estar em branco");
        }
        if (tamanhoBytes < 0) {
            throw new IllegalArgumentException("O tamanho em bytes não pode ser negativo");
        }
    }
}
