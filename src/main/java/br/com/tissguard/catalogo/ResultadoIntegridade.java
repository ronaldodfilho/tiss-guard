package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.EstadoIntegridade;

import java.util.Objects;

public record ResultadoIntegridade(
        String idArtefato,
        EstadoIntegridade estado,
        String hashEsperado,
        String hashCalculado,
        String mensagem
) {
    public ResultadoIntegridade {
        Objects.requireNonNull(idArtefato, "O identificador do artefato não pode ser nulo");
        Objects.requireNonNull(estado, "O estado de integridade não pode ser nulo");
        Objects.requireNonNull(hashEsperado, "O hash esperado não pode ser nulo");
        Objects.requireNonNull(mensagem, "A mensagem não pode ser nula");
    }

    public boolean isIntegro() {
        return estado == EstadoIntegridade.INTEGRO;
    }
}
