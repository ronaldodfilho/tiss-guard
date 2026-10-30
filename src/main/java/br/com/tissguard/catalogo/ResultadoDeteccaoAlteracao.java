package br.com.tissguard.catalogo;

import java.util.Objects;

public record ResultadoDeteccaoAlteracao(
        TipoAlteracaoArtefato tipo,
        String mensagem,
        String hashExistente,
        String hashNovo
) {
    public ResultadoDeteccaoAlteracao {
        Objects.requireNonNull(tipo, "O tipo de alteração não pode ser nulo");
        Objects.requireNonNull(mensagem, "A mensagem não pode ser nula");
    }

    public boolean exigeAtencao() {
        return tipo == TipoAlteracaoArtefato.CONTEUDO_MODIFICADO;
    }

    public boolean isEvolucaoOuNovo() {
        return tipo == TipoAlteracaoArtefato.NOVA_VERSAO || tipo == TipoAlteracaoArtefato.NOVO_ARTEFATO;
    }
}
