package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetectorAlteracoesArtefatoTest {

    private CatalogoArtefatos catalogo;
    private DetectorAlteracoesArtefato detector;

    @BeforeEach
    void configurar() {
        catalogo = new CatalogoArtefatos();
        detector = new DetectorAlteracoesArtefato(catalogo);
    }

    private MetadadosArtefato criarArtefato(String id, ComponenteTiss componente, String versao, String arquivo, String hash) {
        return new MetadadosArtefato(
                id,
                "Artefato " + arquivo,
                componente,
                versao,
                "https://gov.br/ans",
                LocalDate.now(),
                LocalDate.of(2024, 4, 1),
                arquivo,
                hash,
                EstadoIntegridade.INTEGRO,
                "Teste"
        );
    }

    @Test
    void deveDetectarNovoArtefatoQuandoComponenteNaoPossuiRegistros() {
        MetadadosArtefato novo = criarArtefato("art-1", ComponenteTiss.COMUNICACAO, "04.03.00", "tissV4_03_00.xsd", "hash1");

        ResultadoDeteccaoAlteracao resultado = detector.detectar(novo);

        assertThat(resultado.tipo()).isEqualTo(TipoAlteracaoArtefato.NOVO_ARTEFATO);
        assertThat(resultado.isEvolucaoOuNovo()).isTrue();
        assertThat(resultado.exigeAtencao()).isFalse();
        assertThat(resultado.hashExistente()).isNull();
        assertThat(resultado.hashNovo()).isEqualTo("hash1");
    }

    @Test
    void deveDetectarNovaVersaoQuandoComponenteJaExisteMasVersaoOuArquivoSaoNovos() {
        MetadadosArtefato existente = criarArtefato("art-1", ComponenteTiss.COMUNICACAO, "04.03.00", "tissV4_03_00.xsd", "hash1");
        catalogo.registrarArtefato(existente);

        MetadadosArtefato novaVersao = criarArtefato("art-2", ComponenteTiss.COMUNICACAO, "04.04.00", "tissV4_04_00.xsd", "hash2");

        ResultadoDeteccaoAlteracao resultado = detector.detectar(novaVersao);

        assertThat(resultado.tipo()).isEqualTo(TipoAlteracaoArtefato.NOVA_VERSAO);
        assertThat(resultado.isEvolucaoOuNovo()).isTrue();
        assertThat(resultado.exigeAtencao()).isFalse();
        assertThat(resultado.hashNovo()).isEqualTo("hash2");
    }

    @Test
    void deveDetectarSemAlteracaoQuandoVersaoArquivoEHashForemIdenticos() {
        MetadadosArtefato existente = criarArtefato("art-1", ComponenteTiss.COMUNICACAO, "04.03.00", "tissV4_03_00.xsd", "hash1");
        catalogo.registrarArtefato(existente);

        MetadadosArtefato mesmoArtefato = criarArtefato("art-99", ComponenteTiss.COMUNICACAO, "04.03.00", "tissV4_03_00.xsd", "hash1");

        ResultadoDeteccaoAlteracao resultado = detector.detectar(mesmoArtefato);

        assertThat(resultado.tipo()).isEqualTo(TipoAlteracaoArtefato.SEM_ALTERACAO);
        assertThat(resultado.exigeAtencao()).isFalse();
        assertThat(resultado.hashExistente()).isEqualTo("hash1");
        assertThat(resultado.hashNovo()).isEqualTo("hash1");
    }

    @Test
    void deveDetectarConteudoModificadoQuandoHashForDiferenteParaMesmaVersaoEArquivo() {
        MetadadosArtefato existente = criarArtefato("art-1", ComponenteTiss.COMUNICACAO, "04.03.00", "tissV4_03_00.xsd", "hashOriginal");
        catalogo.registrarArtefato(existente);

        MetadadosArtefato alterado = criarArtefato("art-99", ComponenteTiss.COMUNICACAO, "04.03.00", "tissV4_03_00.xsd", "hashModificadoSilenciosamente");

        ResultadoDeteccaoAlteracao resultado = detector.detectar(alterado);

        assertThat(resultado.tipo()).isEqualTo(TipoAlteracaoArtefato.CONTEUDO_MODIFICADO);
        assertThat(resultado.exigeAtencao()).isTrue();
        assertThat(resultado.hashExistente()).isEqualTo("hashOriginal");
        assertThat(resultado.hashNovo()).isEqualTo("hashModificadoSilenciosamente");
        assertThat(resultado.mensagem()).contains("Risco de alteração silenciosa");
    }

    @Test
    void deveLancarExcecaoQuandoNovoArtefatoForNulo() {
        assertThatThrownBy(() -> detector.detectar(null))
                .isInstanceOf(NullPointerException.class);
    }
}
