package br.com.tissguard.validacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorXmlTest {

    private ValidadorXml validador;
    private LeitorXmlSeguro leitor;

    @BeforeEach
    void configurar() {
        leitor = new LeitorXmlSeguro();
        validador = new ValidadorXml(leitor);
    }

    private byte[] carregarRecurso(String caminho) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (stream == null) {
                throw new IllegalArgumentException("Recurso não encontrado: " + caminho);
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler recurso de teste: " + caminho, e);
        }
    }

    private Path obterCaminhoRecurso(String caminho) {
        try {
            return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(caminho)).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Erro ao resolver URI do recurso: " + caminho, e);
        }
    }

    @Test
    void deveValidarXmlCorretoContraXsdComSucesso() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_valida.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        DocumentoXml documento = leitor.ler("mensagem_tiss_valida.xml", xmlBytes);
        ResultadoValidacao resultado = validador.validar(
                documento,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.isValido()).isTrue();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.VALIDO);
        assertThat(resultado.nomeArquivoXml()).isEqualTo("mensagem_tiss_valida.xml");
        assertThat(resultado.versaoEsquema()).isEqualTo("04.03.00");
        assertThat(resultado.erros()).isEmpty();
        assertThat(resultado.mensagemInformativa()).contains("válido de acordo com o esquema XSD");
    }

    @Test
    void deveIdentificarErrosQuandoXmlForInvalidoParaEsquema() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_invalida_esquema.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        DocumentoXml documento = leitor.ler("mensagem_tiss_invalida_esquema.xml", xmlBytes);
        ResultadoValidacao resultado = validador.validar(
                documento,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.INVALIDO);
        assertThat(resultado.erros()).isNotEmpty();

        ErroValidacao primeiroErro = resultado.erros().get(0);
        assertThat(primeiroErro.linha()).isPositive();
        assertThat(primeiroErro.coluna()).isPositive();
        assertThat(primeiroErro.mensagem()).isNotBlank();
    }

    @Test
    void deveValidarPassandoCaminhoDoArquivoXsd() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_valida.xml");
        Path caminhoXsd = obterCaminhoRecurso("esquemas_xsd/tissV4_03_00.xsd");

        DocumentoXml documento = leitor.ler("mensagem_tiss_valida.xml", xmlBytes);
        ResultadoValidacao resultado = validador.validar(documento, "04.03.00", caminhoXsd);

        assertThat(resultado.isValido()).isTrue();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.VALIDO);
    }

    @Test
    void deveLancarExcecaoQuandoEsquemaNaoExistir() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_valida.xml");
        DocumentoXml documento = leitor.ler("mensagem_tiss_valida.xml", xmlBytes);
        Path caminhoInexistente = Path.of("caminho_inexistente_tiss.xsd");

        assertThatThrownBy(() -> validador.validar(documento, "04.03.00", caminhoInexistente))
                .isInstanceOf(EsquemaNaoEncontradoException.class)
                .hasMessageContaining("Arquivo de esquema XSD não encontrado");
    }

    @Test
    void deveLancarExcecaoQuandoEsquemaXsdForInvalido() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_valida.xml");
        byte[] xsdInvalidoBytes = carregarRecurso("esquemas_xsd/esquema_malformado.xsd");

        DocumentoXml documento = leitor.ler("mensagem_tiss_valida.xml", xmlBytes);

        assertThatThrownBy(() -> validador.validar(
                documento,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdInvalidoBytes))
        )).isInstanceOf(EsquemaInvalidoException.class)
          .hasMessageContaining("Erro ao compilar esquema XSD");
    }

    @Test
    void deveValidarXmlDiretamenteAPartirDeString() {
        String xmlValido = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ans:mensagemTISS xmlns:ans="http://www.ans.gov.br/padroes/tiss/schemas">
                    <ans:cabecalho>
                        <ans:identificacaoTransacao>
                            <ans:tipoTransacao>ENVIO_LOTE_GUIAS</ans:tipoTransacao>
                            <ans:sequencialTransacao>2002</ans:sequencialTransacao>
                            <ans:dataRegistroTransacao>2026-10-25</ans:dataRegistroTransacao>
                            <ans:horaRegistroTransacao>10:00:00</ans:horaRegistroTransacao>
                        </ans:identificacaoTransacao>
                        <ans:origem>
                            <ans:identificacaoPrestador>
                                <ans:codigoPrestadorNaOperadora>PREST_123</ans:codigoPrestadorNaOperadora>
                            </ans:identificacaoPrestador>
                        </ans:origem>
                        <ans:destino>
                            <ans:registroANS>654321</ans:registroANS>
                        </ans:destino>
                        <ans:versaoPadrao>04.03.00</ans:versaoPadrao>
                    </ans:cabecalho>
                </ans:mensagemTISS>
                """;

        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");
        ResultadoValidacao resultado = validador.validar(
                "mensagem_direta.xml",
                xmlValido,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado.isValido()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoArgumentosObrigatoriosForemNulosOuVazios() {
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");
        StreamSource fonte = new StreamSource(new ByteArrayInputStream(xsdBytes));

        assertThatThrownBy(() -> validador.validar((DocumentoXml) null, "04.03.00", fonte))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> validador.validar("teste.xml", "<xml/>", "", fonte))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> validador.validar("teste.xml", "<xml/>", "04.03.00", (StreamSource) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveGarantirImutabilidadeDaListaDeErrosNoResultado() {
        ErroValidacao erro = new ErroValidacao(10, 5, "Campo obrigatório ausente");
        ResultadoValidacao resultado = new ResultadoValidacao(
                "arquivo.xml",
                "04.03.00",
                StatusValidacao.INVALIDO,
                java.util.List.of(erro),
                "Mensagem"
        );

        assertThatThrownBy(() -> resultado.erros().add(new ErroValidacao(11, 2, "Outro erro")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deveTratarXmlMalformadoRetornandoStatusNaoConcluidoECategoriaSintaxe() {
        byte[] xmlMalformadoBytes = carregarRecurso("exemplos_xml/xml_malformado.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        ResultadoValidacao resultado = validador.validar(
                "xml_malformado.xml",
                xmlMalformadoBytes,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.NAO_CONCLUIDO);
        assertThat(resultado.temErroSintaxe()).isTrue();
        assertThat(resultado.temErroEsquema()).isFalse();
        assertThat(resultado.erros()).hasSize(1);

        ErroValidacao erro = resultado.erros().get(0);
        assertThat(erro.categoria()).isEqualTo(CategoriaErroValidacao.SINTAXE_XML);
        assertThat(erro.linha()).isPositive();
        assertThat(erro.coluna()).isPositive();
        assertThat(erro.mensagem()).contains("malformado");
    }

    @Test
    void deveTratarXmlComXxeRetornandoStatusNaoConcluidoECategoriaSeguranca() {
        byte[] xmlXxeBytes = carregarRecurso("exemplos_xml/xml_com_xxe.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        ResultadoValidacao resultado = validador.validar(
                "xml_com_xxe.xml",
                xmlXxeBytes,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.NAO_CONCLUIDO);
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).categoria()).isEqualTo(CategoriaErroValidacao.SEGURANCA);
        assertThat(resultado.mensagemInformativa()).contains("segurança");
    }

    @Test
    void deveTraduzirMensagemQuandoElementoObrigatorioEstiverAusente() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_elemento_faltante.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        ResultadoValidacao resultado = validador.validar(
                "mensagem_tiss_elemento_faltante.xml",
                xmlBytes,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.INVALIDO);
        assertThat(resultado.temErroEsquema()).isTrue();
        assertThat(resultado.temErroSintaxe()).isFalse();
        assertThat(resultado.erros()).isNotEmpty();

        ErroValidacao primeiroErro = resultado.erros().get(0);
        assertThat(primeiroErro.categoria()).isEqualTo(CategoriaErroValidacao.ESQUEMA_XSD);
        assertThat(primeiroErro.mensagem()).contains("Elemento obrigatório ausente ou incompleto");
        assertThat(primeiroErro.mensagem()).contains("versaoPadrao");
    }

    @Test
    void deveTraduzirMensagemQuandoElementoInesperadoForEncontrado() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_elemento_inesperado.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        ResultadoValidacao resultado = validador.validar(
                "mensagem_tiss_elemento_inesperado.xml",
                xmlBytes,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.INVALIDO);
        assertThat(resultado.temErroEsquema()).isTrue();
        assertThat(resultado.erros()).isNotEmpty();

        ErroValidacao primeiroErro = resultado.erros().get(0);
        assertThat(primeiroErro.categoria()).isEqualTo(CategoriaErroValidacao.ESQUEMA_XSD);
        assertThat(primeiroErro.mensagem()).contains("Elemento inesperado");
        assertThat(primeiroErro.mensagem()).contains("elementoInesperado");
    }

    @Test
    void deveTraduzirMensagemQuandoValorNaoAtenderAoPadraoOuTipo() {
        byte[] xmlBytes = carregarRecurso("exemplos_xml/mensagem_tiss_invalida_esquema.xml");
        byte[] xsdBytes = carregarRecurso("esquemas_xsd/tissV4_03_00.xsd");

        ResultadoValidacao resultado = validador.validar(
                "mensagem_tiss_invalida_esquema.xml",
                xmlBytes,
                "04.03.00",
                new StreamSource(new ByteArrayInputStream(xsdBytes))
        );

        assertThat(resultado).isNotNull();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.INVALIDO);
        assertThat(resultado.erros()).isNotEmpty();

        boolean encontrouPadraoOuTipo = resultado.erros().stream()
                .anyMatch(erro -> erro.mensagem().contains("Valor inválido") || erro.mensagem().contains("Tipo de dado incompatível"));
        assertThat(encontrouPadraoOuTipo).isTrue();
    }
}
