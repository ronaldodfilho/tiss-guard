package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoArtefatosTest {

    private CatalogoArtefatos catalogo;

    @BeforeEach
    void configurar() {
        catalogo = new CatalogoArtefatos();
    }

    private MetadadosArtefato criarArtefatoExemplo(String id, String nomeArquivo) {
        return new MetadadosArtefato(
                id,
                "Esquema Principal TISS",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://www.gov.br/ans/pt-br",
                LocalDate.of(2026, 10, 22),
                LocalDate.of(2024, 4, 1),
                nomeArquivo,
                "4f53c150c226a26e85e2b9c7cf8eb6116fce8f0ad562df8a2f4efbc4e6a8e8b0",
                EstadoIntegridade.INTEGRO,
                "Esquema para testes"
        );
    }

    @Test
    void deveRegistrarArtefatoComSucesso() {
        MetadadosArtefato artefato = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");

        catalogo.registrarArtefato(artefato);

        assertThat(catalogo.quantidadeArtefatos()).isEqualTo(1);
        Optional<MetadadosArtefato> encontrado = catalogo.buscarPorId("art-1");
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().nomeArquivo()).isEqualTo("tissV4_03_00.xsd");
    }

    @Test
    void deveLancarExcecaoAoRegistrarArtefatoComIdDuplicado() {
        MetadadosArtefato artefato1 = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        MetadadosArtefato artefato2 = criarArtefatoExemplo("art-1", "outroArquivo.xsd");

        catalogo.registrarArtefato(artefato1);

        assertThatThrownBy(() -> catalogo.registrarArtefato(artefato2))
                .isInstanceOf(ArtefatoJaRegistradoException.class)
                .hasMessageContaining("art-1");
    }

    @Test
    void deveLancarExcecaoAoRegistrarArtefatoComMesmoComponenteVersaoEArquivo() {
        MetadadosArtefato artefato1 = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        MetadadosArtefato artefato2 = criarArtefatoExemplo("art-2", "tissV4_03_00.xsd");

        catalogo.registrarArtefato(artefato1);

        assertThatThrownBy(() -> catalogo.registrarArtefato(artefato2))
                .isInstanceOf(ArtefatoJaRegistradoException.class)
                .hasMessageContaining("Já existe um artefato cadastrado para o componente");
    }

    @Test
    void deveLancarExcecaoAoCriarArtefatoComCamposObrigatoriosInvalidos() {
        assertThatThrownBy(() -> new MetadadosArtefato(
                "",
                "Nome",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://gov.br",
                LocalDate.now(),
                null,
                "arquivo.xsd",
                "hash",
                EstadoIntegridade.INTEGRO,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveBuscarArtefatosPorComponente() {
        MetadadosArtefato artefatoComunicacao = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        catalogo.registrarArtefato(artefatoComunicacao);

        List<MetadadosArtefato> resultado = catalogo.buscarPorComponente(ComponenteTiss.COMUNICACAO);
        assertThat(resultado).hasSize(1);

        List<MetadadosArtefato> vazia = catalogo.buscarPorComponente(ComponenteTiss.ORGANIZACIONAL);
        assertThat(vazia).isEmpty();
    }

    @Test
    void deveBuscarArtefatoPorVersaoENomeArquivo() {
        MetadadosArtefato artefato = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        catalogo.registrarArtefato(artefato);

        Optional<MetadadosArtefato> encontrado = catalogo.buscarPorVersaoENomeArquivo("04.03.00", "tissv4_03_00.xsd");
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().id()).isEqualTo("art-1");
    }

    @Test
    void deveRetornarListaImutavelAoListarTodos() {
        MetadadosArtefato artefato = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        catalogo.registrarArtefato(artefato);

        List<MetadadosArtefato> todos = catalogo.listarTodos();

        assertThatThrownBy(() -> todos.add(artefato))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deveListarVersoesDistintasPorComponente() {
        MetadadosArtefato artefatoV403 = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        MetadadosArtefato artefatoV403Guias = new MetadadosArtefato(
                "art-2",
                "Guias TISS",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://gov.br",
                LocalDate.now(),
                LocalDate.now(),
                "tissGuiasV4_03_00.xsd",
                "hash2",
                EstadoIntegridade.INTEGRO,
                "Obs"
        );
        MetadadosArtefato artefatoV404 = new MetadadosArtefato(
                "art-3",
                "Esquema V4.04",
                ComponenteTiss.COMUNICACAO,
                "04.04.00",
                "https://gov.br",
                LocalDate.now(),
                LocalDate.now(),
                "tissV4_04_00.xsd",
                "hash3",
                EstadoIntegridade.INTEGRO,
                "Obs"
        );

        catalogo.registrarArtefato(artefatoV403);
        catalogo.registrarArtefato(artefatoV403Guias);
        catalogo.registrarArtefato(artefatoV404);

        List<String> versoes = catalogo.listarVersoesPorComponente(ComponenteTiss.COMUNICACAO);

        assertThat(versoes).containsExactly("04.03.00", "04.04.00");
    }

    @Test
    void deveBuscarArtefatosPorVersao() {
        MetadadosArtefato artefato1 = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        catalogo.registrarArtefato(artefato1);

        List<MetadadosArtefato> encontrados = catalogo.buscarPorVersao("04.03.00");
        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.get(0).id()).isEqualTo("art-1");

        List<MetadadosArtefato> naoEncontrados = catalogo.buscarPorVersao("04.99.99");
        assertThat(naoEncontrados).isEmpty();
    }

    @Test
    void deveVerificarSePossuiVersaoCadastrada() {
        MetadadosArtefato artefato1 = criarArtefatoExemplo("art-1", "tissV4_03_00.xsd");
        catalogo.registrarArtefato(artefato1);

        assertThat(catalogo.possuiVersao("04.03.00")).isTrue();
        assertThat(catalogo.possuiVersao("04.04.00")).isFalse();
        assertThat(catalogo.possuiVersao(null)).isFalse();
        assertThat(catalogo.possuiVersao("   ")).isFalse();
    }

    @Test
    void deveRetornarColecoesVaziasParaParametrosNulosOuEmBranco() {
        assertThat(catalogo.listarVersoesPorComponente(null)).isEmpty();
        assertThat(catalogo.buscarPorVersao(null)).isEmpty();
        assertThat(catalogo.buscarPorVersao("   ")).isEmpty();
    }
}

