package br.com.tissguard.api.dto;

import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;

import java.time.LocalDate;
import java.util.Objects;

public record ArtefatoCatalogoDto(
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
    public ArtefatoCatalogoDto {
        Objects.requireNonNull(id, "O identificador não pode ser nulo");
        Objects.requireNonNull(nome, "O nome não pode ser nulo");
        Objects.requireNonNull(componente, "O componente não pode ser nulo");
        Objects.requireNonNull(versaoDeclarada, "A versão não pode ser nula");
        Objects.requireNonNull(enderecoOrigem, "O endereço de origem não pode ser nulo");
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");
        Objects.requireNonNull(hashSha256, "O hash SHA-256 não pode ser nulo");
        Objects.requireNonNull(estadoIntegridade, "O estado de integridade não pode ser nulo");
    }

    public static ArtefatoCatalogoDto de(MetadadosArtefato artefato) {
        Objects.requireNonNull(artefato, "O artefato não pode ser nulo");
        return new ArtefatoCatalogoDto(
                artefato.id(),
                artefato.nome(),
                artefato.componente(),
                artefato.versaoDeclarada(),
                artefato.enderecoOrigem(),
                artefato.dataConsulta(),
                artefato.dataPublicacao(),
                artefato.nomeArquivo(),
                artefato.hashSha256(),
                artefato.estadoIntegridade(),
                artefato.observacoes()
        );
    }
}
