package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.MetadadosArtefato;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class DetectorAlteracoesArtefato {

    private final CatalogoArtefatos catalogo;

    public DetectorAlteracoesArtefato(CatalogoArtefatos catalogo) {
        this.catalogo = Objects.requireNonNull(catalogo, "O catálogo de artefatos não pode ser nulo");
    }

    public ResultadoDeteccaoAlteracao detectar(MetadadosArtefato novoArtefato) {
        Objects.requireNonNull(novoArtefato, "O novo artefato não pode ser nulo");

        Optional<MetadadosArtefato> existente = catalogo.buscarPorVersaoENomeArquivo(
                novoArtefato.versaoDeclarada(),
                novoArtefato.nomeArquivo()
        );

        if (existente.isEmpty()) {
            boolean existeComponente = !catalogo.buscarPorComponente(novoArtefato.componente()).isEmpty();
            if (existeComponente) {
                return new ResultadoDeteccaoAlteracao(
                        TipoAlteracaoArtefato.NOVA_VERSAO,
                        "Nova versão ou arquivo detectado para o componente " + novoArtefato.componente() +
                        ": versão " + novoArtefato.versaoDeclarada() + ", arquivo " + novoArtefato.nomeArquivo(),
                        null,
                        novoArtefato.hashSha256()
                );
            }

            return new ResultadoDeteccaoAlteracao(
                    TipoAlteracaoArtefato.NOVO_ARTEFATO,
                    "Novo artefato identificado para componente não registrado previamente: " + novoArtefato.componente(),
                    null,
                    novoArtefato.hashSha256()
            );
        }

        MetadadosArtefato artefatoCadastrado = existente.get();
        if (artefatoCadastrado.hashSha256().equalsIgnoreCase(novoArtefato.hashSha256())) {
            return new ResultadoDeteccaoAlteracao(
                    TipoAlteracaoArtefato.SEM_ALTERACAO,
                    "Artefato inalterado. O hash SHA-256 é idêntico ao já registrado no catálogo.",
                    artefatoCadastrado.hashSha256(),
                    novoArtefato.hashSha256()
            );
        }

        return new ResultadoDeteccaoAlteracao(
                TipoAlteracaoArtefato.CONTEUDO_MODIFICADO,
                "Alerta: modificação de conteúdo detectada para a mesma versão (" + novoArtefato.versaoDeclarada() +
                ") e arquivo (" + novoArtefato.nomeArquivo() + "). Risco de alteração silenciosa na fonte oficial.",
                artefatoCadastrado.hashSha256(),
                novoArtefato.hashSha256()
        );
    }
}
