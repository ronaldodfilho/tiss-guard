package br.com.tissguard.dominio;

import java.time.LocalDate;
import java.util.Objects;

public record MetadadosArtefato(
        String id,
        String nome,
        ComponenteTiss componente,
        String versaoDeclarada,
        String enderecoOrigem,
        LocalDate dataConsulta,
        LocalDate dataPublicacao,
        String nomeArquivo,
        String hashSha256,
        EstadoIntegridade estadoIntegridade,
        String observacoes
) {
    public MetadadosArtefato {
        Objects.requireNonNull(id, "O identificador do artefato não pode ser nulo");
        Objects.requireNonNull(nome, "O nome do artefato não pode ser nulo");
        Objects.requireNonNull(componente, "O componente TISS não pode ser nulo");
        Objects.requireNonNull(versaoDeclarada, "A versão declarada não pode ser nula");
        Objects.requireNonNull(enderecoOrigem, "O endereço de origem não pode ser nulo");
        Objects.requireNonNull(dataConsulta, "A data de consulta não pode ser nula");
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");
        Objects.requireNonNull(hashSha256, "O hash SHA-256 não pode ser nulo");
        Objects.requireNonNull(estadoIntegridade, "O estado de integridade não pode ser nulo");

        if (id.isBlank()) {
            throw new IllegalArgumentException("O identificador do artefato não pode estar em branco");
        }
        if (nome.isBlank()) {
            throw new IllegalArgumentException("O nome do artefato não pode estar em branco");
        }
        if (versaoDeclarada.isBlank()) {
            throw new IllegalArgumentException("A versão declarada não pode estar em branco");
        }
        if (enderecoOrigem.isBlank()) {
            throw new IllegalArgumentException("O endereço de origem não pode estar em branco");
        }
        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo não pode estar em branco");
        }
        if (hashSha256.isBlank()) {
            throw new IllegalArgumentException("O hash SHA-256 não pode estar em branco");
        }
    }
}
