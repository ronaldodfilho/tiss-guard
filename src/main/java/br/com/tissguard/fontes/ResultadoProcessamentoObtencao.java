package br.com.tissguard.fontes;

import br.com.tissguard.catalogo.ResultadoDeteccaoAlteracao;
import br.com.tissguard.dominio.MetadadosArtefato;

import java.nio.file.Path;
import java.util.Objects;

public record ResultadoProcessamentoObtencao(
        MetadadosArtefato metadados,
        ResultadoDeteccaoAlteracao deteccao,
        Path caminhoArquivoSalvo,
        boolean arquivoGravado
) {
    public ResultadoProcessamentoObtencao {
        Objects.requireNonNull(metadados, "Os metadados não podem ser nulos");
        Objects.requireNonNull(deteccao, "O resultado da detecção não pode ser nulo");
    }
}
