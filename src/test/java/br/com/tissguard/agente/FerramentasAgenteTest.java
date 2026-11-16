package br.com.tissguard.agente;

import br.com.tissguard.api.dto.ArtefatoCatalogoDto;
import br.com.tissguard.api.dto.ResultadoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.ResultadoValidacaoXmlDto;
import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.comparacao.ComparadorEsquemas;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import br.com.tissguard.validacao.LocalizadorEsquemas;
import br.com.tissguard.validacao.ServicoValidacao;
import br.com.tissguard.validacao.StatusValidacao;
import br.com.tissguard.validacao.ValidadorXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FerramentasAgenteTest {

    private CatalogoArtefatos catalogo;
    private FerramentaConsultaArtefatos ferramentaConsulta;
    private FerramentaValidacaoXml ferramentaValidacao;
    private FerramentaComparacaoEsquemas ferramentaComparacao;

    @BeforeEach
    void configurar() {
        catalogo = new CatalogoArtefatos();
        MetadadosArtefato artefato = new MetadadosArtefato(
                "art-1",
                "Esquema 04.03.00",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://ans.gov.br",
                LocalDate.now(),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "hash1",
                EstadoIntegridade.INTEGRO,
                "Obs"
        );
        catalogo.registrarArtefato(artefato);

        LocalizadorEsquemas localizador = new LocalizadorEsquemas();
        ValidadorXml validador = new ValidadorXml();
        ServicoValidacao servicoValidacao = new ServicoValidacao(validador, localizador);
        ComparadorEsquemas comparadorEsquemas = new ComparadorEsquemas();

        ferramentaConsulta = new FerramentaConsultaArtefatos(catalogo);
        ferramentaValidacao = new FerramentaValidacaoXml(servicoValidacao);
        ferramentaComparacao = new FerramentaComparacaoEsquemas(comparadorEsquemas, localizador);
    }

    private String lerRecurso(String caminho) {
        try (InputStream is = getClass().getResourceAsStream(caminho)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deveExecutarFerramentaConsultaArtefatos() {
        List<ArtefatoCatalogoDto> todos = ferramentaConsulta.executar(null);
        assertThat(todos).hasSize(1);
        assertThat(todos.getFirst().id()).isEqualTo("art-1");

        List<ArtefatoCatalogoDto> porComponente = ferramentaConsulta.executar(
                new FerramentaConsultaArtefatos.Parametros(ComponenteTiss.COMUNICACAO, null)
        );
        assertThat(porComponente).hasSize(1);

        List<ArtefatoCatalogoDto> porVersao = ferramentaConsulta.executar(
                new FerramentaConsultaArtefatos.Parametros(null, "04.03.00")
        );
        assertThat(porVersao).hasSize(1);
    }

    @Test
    void deveExecutarFerramentaValidacaoXml() {
        String xml = lerRecurso("/exemplos_xml/mensagem_tiss_valida.xml");
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "guia.xml",
                "04.03.00",
                xml
        );

        ResultadoValidacaoXmlDto resultado = ferramentaValidacao.executar(solicitacao);

        assertThat(resultado).isNotNull();
        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.VALIDO);
    }

    @Test
    void deveExecutarFerramentaComparacaoEsquemas() {
        SolicitacaoComparacaoEsquemasDto solicitacao = new SolicitacaoComparacaoEsquemasDto(
                "04.03.00",
                "tissV4_03_00.xsd",
                "04.04.00",
                "tissV4_04_00_evolucao.xsd"
        );

        ResultadoComparacaoEsquemasDto resultado = ferramentaComparacao.executar(solicitacao);

        assertThat(resultado).isNotNull();
        assertThat(resultado.esquemasIdenticos()).isFalse();
        assertThat(resultado.totalQuebrasContrato()).isGreaterThan(0);
    }
}

