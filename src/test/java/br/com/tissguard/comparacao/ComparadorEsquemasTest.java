package br.com.tissguard.comparacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparadorEsquemasTest {

    private ComparadorEsquemas comparador;
    private Path caminhoV40300;
    private Path caminhoV40400;

    @BeforeEach
    void configurar() {
        comparador = new ComparadorEsquemas();
        caminhoV40300 = Path.of("src/test/resources/esquemas_xsd/tissV4_03_00.xsd");
        caminhoV40400 = Path.of("src/test/resources/esquemas_xsd/tissV4_04_00_evolucao.xsd");
    }

    @Test
    void deveRetornarZeroDiferencasQuandoCompararEsquemaConsigoMesmo() {
        ResultadoComparacaoEsquema resultado = comparador.comparar(
                caminhoV40300,
                "04.03.00",
                caminhoV40300,
                "04.03.00"
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.esquemasIdenticos()).isTrue();
        assertThat(resultado.totalDiferencas()).isZero();
        assertThat(resultado.diferencas()).isEmpty();
        assertThat(resultado.versaoEsquemaBase()).isEqualTo("04.03.00");
        assertThat(resultado.versaoEsquemaComparado()).isEqualTo("04.03.00");
        assertThat(resultado.hashEsquemaBase()).isEqualTo(resultado.hashEsquemaComparado());
    }

    @Test
    void deveDetectarDiferencasEstruturaisConhecidasEntreVersoesDistintas() {
        ResultadoComparacaoEsquema resultado = comparador.comparar(
                caminhoV40300,
                "04.03.00",
                caminhoV40400,
                "04.04.00"
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.esquemasIdenticos()).isFalse();
        assertThat(resultado.totalDiferencas()).isGreaterThan(0);

        List<DiferencaEsquema> adicionados = resultado.diferencasPorTipo(TipoDiferencaEsquema.ELEMENTO_ADICIONADO);
        assertThat(adicionados)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains(
                        "mensagemTISS/epilogo",
                        "ct_cabecalho/observacao",
                        "ct_epilogo/hashControle"
                );

        List<DiferencaEsquema> removidos = resultado.diferencasPorTipo(TipoDiferencaEsquema.ELEMENTO_REMOVIDO);
        assertThat(removidos)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains("ct_identificacaoTransacao/horaRegistroTransacao");

        List<DiferencaEsquema> modificados = resultado.diferencasPorTipo(TipoDiferencaEsquema.ELEMENTO_MODIFICADO);
        assertThat(modificados)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains(
                        "ct_cabecalho/origem",
                        "ct_identificacaoTransacao/tipoTransacao"
                );

        DiferencaEsquema modTipo = modificados.stream()
                .filter(d -> d.caminhoEstrutural().equals("ct_identificacaoTransacao/tipoTransacao"))
                .findFirst()
                .orElseThrow();
        assertThat(modTipo.valorAnterior()).isEqualTo("xs:string");
        assertThat(modTipo.valorNovo()).isEqualTo("ans:st_tipoTransacao");

        DiferencaEsquema modMinOccurs = modificados.stream()
                .filter(d -> d.caminhoEstrutural().equals("ct_cabecalho/origem"))
                .findFirst()
                .orElseThrow();
        assertThat(modMinOccurs.valorAnterior()).isEqualTo("1");
        assertThat(modMinOccurs.valorNovo()).isEqualTo("0");

        List<DiferencaEsquema> tiposAdicionados = resultado.diferencasPorTipo(TipoDiferencaEsquema.TIPO_ADICIONADO);
        assertThat(tiposAdicionados)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains("ct_epilogo", "st_tipoTransacao");

        List<DiferencaEsquema> tiposModificados = resultado.diferencasPorTipo(TipoDiferencaEsquema.TIPO_MODIFICADO);
        assertThat(tiposModificados)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains("st_versao");

        DiferencaEsquema modVersao = tiposModificados.stream()
                .filter(d -> d.caminhoEstrutural().equals("st_versao"))
                .findFirst()
                .orElseThrow();
        assertThat(modVersao.valorAnterior()).isEqualTo("xs:string");
        assertThat(modVersao.valorNovo()).isEqualTo("xs:token");
    }

    @Test
    void devePreservarReferenciasAosArquivosEHashesUtilizados() {
        ResultadoComparacaoEsquema resultado = comparador.comparar(
                caminhoV40300,
                "04.03.00",
                caminhoV40400,
                "04.04.00"
        );

        assertThat(resultado.identificadorEsquemaBase()).isEqualTo("tissV4_03_00.xsd");
        assertThat(resultado.versaoEsquemaBase()).isEqualTo("04.03.00");
        assertThat(resultado.hashEsquemaBase()).isNotBlank().hasSize(64);

        assertThat(resultado.identificadorEsquemaComparado()).isEqualTo("tissV4_04_00_evolucao.xsd");
        assertThat(resultado.versaoEsquemaComparado()).isEqualTo("04.04.00");
        assertThat(resultado.hashEsquemaComparado()).isNotBlank().hasSize(64);

        assertThat(resultado.hashEsquemaBase()).isNotEqualTo(resultado.hashEsquemaComparado());
    }

    @Test
    void deveLancarExcecaoQuandoArquivoInformadoNaoForEsquemaXml() {
        Path caminhoXmlExemplo = Path.of("src/test/resources/exemplos_xml/mensagem_tiss_valida.xml");

        assertThatThrownBy(() -> comparador.comparar(
                caminhoXmlExemplo,
                "04.03.00",
                caminhoV40300,
                "04.03.00"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não é um esquema XML válido");
    }

    @Test
    void deveLancarExcecaoQuandoCaminhoNaoExistir() {
        Path caminhoInexistente = Path.of("src/test/resources/esquemas_xsd/arquivo_que_nao_existe.xsd");

        assertThatThrownBy(() -> comparador.comparar(
                caminhoInexistente,
                "04.03.00",
                caminhoV40300,
                "04.03.00"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não existe");
    }

    @Test
    void deveValidarParametrosObrigatorios() {
        assertThatThrownBy(() -> comparador.comparar(null, "04.03.00", caminhoV40300, "04.03.00"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> comparador.comparar(caminhoV40300, null, caminhoV40300, "04.03.00"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> comparador.comparar(caminhoV40300, "04.03.00", null, "04.03.00"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> comparador.comparar(caminhoV40300, "04.03.00", caminhoV40300, null))
                .isInstanceOf(NullPointerException.class);
    }
}
