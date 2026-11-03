package br.com.tissguard.fontes;

import br.com.tissguard.dominio.ComponenteTiss;

import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;

public record SolicitacaoObtencaoArtefato(
        String id,
        String nome,
        ComponenteTiss componente,
        String versaoDeclarada,
        URI uriOrigem,
        LocalDate dataPublicacao,
        String nomeArquivo,
        String observacoes
) {
    public SolicitacaoObtencaoArtefato {
        Objects.requireNonNull(id, "O identificador não pode ser nulo");
        Objects.requireNonNull(nome, "O nome não pode ser nulo");
        Objects.requireNonNull(componente, "O componente TISS não pode ser nulo");
        Objects.requireNonNull(versaoDeclarada, "A versão declarada não pode ser nula");
        Objects.requireNonNull(uriOrigem, "A URI de origem não pode ser nula");
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");

        if (id.isBlank()) {
            throw new IllegalArgumentException("O identificador não pode estar em branco");
        }
        if (nome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode estar em branco");
        }
        if (versaoDeclarada.isBlank()) {
            throw new IllegalArgumentException("A versão declarada não pode estar em branco");
        }
        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo não pode estar em branco");
        }
    }
}
