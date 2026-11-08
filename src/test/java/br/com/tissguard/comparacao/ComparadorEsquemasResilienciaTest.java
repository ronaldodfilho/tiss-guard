package br.com.tissguard.comparacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComparadorEsquemasResilienciaTest {

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
    void deveGerarDiagnosticoComQuebraDeContratoQuandoHouverRemocoesOuTiposAlterados() {
        ResultadoComparacaoEsquema resultado = comparador.comparar(
                caminhoV40300,
                "04.03.00",
                caminhoV40400,
                "04.04.00"
        );

        assertThat(resultado.isCompativelComVersaoAnterior()).isFalse();

        DiagnosticoCompatibilidade diagnostico = resultado.diagnostico();
        assertThat(diagnostico.compativelComVersaoAnterior()).isFalse();
        assertThat(diagnostico.totalQuebrasContrato()).isGreaterThan(0);
        assertThat(diagnostico.totalAlteracoesAditivas()).isGreaterThan(0);
        assertThat(diagnostico.resumoDiagnostico()).contains("Alerta: quebra de contrato detectada");
        assertThat(diagnostico.recomendacaoIntegracao()).contains("período de transição");

        List<DiferencaEsquema> quebras = resultado.quebrasDeContrato();
        assertThat(quebras)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains(
                        "ct_identificacaoTransacao/horaRegistroTransacao",
                        "ct_identificacaoTransacao/tipoTransacao"
                );

        List<DiferencaEsquema> aditivas = resultado.alteracoesAditivas();
        assertThat(aditivas)
                .extracting(DiferencaEsquema::caminhoEstrutural)
                .contains(
                        "mensagemTISS/epilogo",
                        "ct_cabecalho/observacao",
                        "ct_cabecalho/origem"
                );
    }

    @Test
    void deveGerarDiagnosticoCompativelQuandoTodasAlteracoesForemAditivas() {
        String xsdBase = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="lote">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element name="codigo" type="xs:string"/>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                </xs:schema>
                """;

        String xsdNovaVersaoAditiva = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="lote">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element name="codigo" type="xs:string"/>
                                <xs:element name="observacaoOpcional" type="xs:string" minOccurs="0"/>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                    <xs:complexType name="tipoComplementar"/>
                </xs:schema>
                """;

        ResultadoComparacaoEsquema resultado = comparador.comparar(
                "lote_v1.xsd",
                "01.00.00",
                "hash1",
                xsdBase.getBytes(StandardCharsets.UTF_8),
                "lote_v2.xsd",
                "01.01.00",
                "hash2",
                xsdNovaVersaoAditiva.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(resultado.isCompativelComVersaoAnterior()).isTrue();
        assertThat(resultado.quebrasDeContrato()).isEmpty();
        assertThat(resultado.alteracoesAditivas()).hasSize(2);

        DiagnosticoCompatibilidade diagnostico = resultado.diagnostico();
        assertThat(diagnostico.compativelComVersaoAnterior()).isTrue();
        assertThat(diagnostico.totalQuebrasContrato()).isZero();
        assertThat(diagnostico.totalAlteracoesAditivas()).isEqualTo(2);
        assertThat(diagnostico.resumoDiagnostico()).contains("Versão compatível retroativamente");
        assertThat(diagnostico.recomendacaoIntegracao()).contains("continuarão funcionando");
    }

    @Test
    void deveClassificarNovoElementoObrigatorioComoQuebraDeContrato() {
        String xsdBase = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="guia">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element name="numero" type="xs:string"/>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                </xs:schema>
                """;

        String xsdComElementoObrigatorio = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                    <xs:element name="guia">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element name="numero" type="xs:string"/>
                                <xs:element name="novoCampoObrigatorio" type="xs:string" minOccurs="1"/>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>
                </xs:schema>
                """;

        ResultadoComparacaoEsquema resultado = comparador.comparar(
                "guia_v1.xsd",
                "01.00.00",
                "hash1",
                xsdBase.getBytes(StandardCharsets.UTF_8),
                "guia_v2.xsd",
                "01.01.00",
                "hash2",
                xsdComElementoObrigatorio.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(resultado.isCompativelComVersaoAnterior()).isFalse();
        assertThat(resultado.quebrasDeContrato()).hasSize(1);

        DiferencaEsquema quebra = resultado.quebrasDeContrato().getFirst();
        assertThat(quebra.caminhoEstrutural()).isEqualTo("guia/novoCampoObrigatorio");
        assertThat(quebra.descricao()).contains("obrigatório");
        assertThat(quebra.severidade()).isEqualTo(SeveridadeDiferenca.QUEBRA_DE_CONTRATO);
    }
}
