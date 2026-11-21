package br.com.tissguard.integracao;

import br.com.tissguard.agente.AgenteInvestigador;
import br.com.tissguard.agente.FerramentaComparacaoEsquemas;
import br.com.tissguard.agente.FerramentaConsultaArtefatos;
import br.com.tissguard.agente.FerramentaValidacaoXml;
import br.com.tissguard.agente.PerguntaInvestigacao;
import br.com.tissguard.agente.RespostaInvestigacao;
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
import br.com.tissguard.validacao.ValidadorXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FluxoDemonstracaoIntegradoTest {

    private CatalogoArtefatos catalogo;
    private ServicoValidacao servicoValidacao;
    private ComparadorEsquemas comparadorEsquemas;
    private LocalizadorEsquemas localizador;
    private AgenteInvestigador agente;

    @BeforeEach
    void configurar() {
        catalogo = new CatalogoArtefatos();
        catalogo.registrarArtefato(new MetadadosArtefato(
                "art-v4-03-00",
                "Esquema Geral TISS 04.03.00",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://ans.gov.br",
                LocalDate.now(),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "hashSha256Oficial403",
                EstadoIntegridade.INTEGRO,
                "Esquema oficial"
        ));
        catalogo.registrarArtefato(new MetadadosArtefato(
                "art-v4-04-00",
                "Esquema Geral TISS 04.04.00",
                ComponenteTiss.COMUNICACAO,
                "04.04.00",
                "https://ans.gov.br",
                LocalDate.now(),
                LocalDate.of(2024, 10, 1),
                "tissV4_04_00_evolucao.xsd",
                "hashSha256Oficial404",
                EstadoIntegridade.INTEGRO,
                "Esquema de evolução"
        ));

        localizador = new LocalizadorEsquemas();
        ValidadorXml validadorXml = new ValidadorXml();
        servicoValidacao = new ServicoValidacao(validadorXml, localizador);
        comparadorEsquemas = new ComparadorEsquemas();

        FerramentaConsultaArtefatos ferramentaConsulta = new FerramentaConsultaArtefatos(catalogo);
        FerramentaValidacaoXml ferramentaValidacao = new FerramentaValidacaoXml(servicoValidacao);
        FerramentaComparacaoEsquemas ferramentaComparacao = new FerramentaComparacaoEsquemas(comparadorEsquemas, localizador);

        agente = new AgenteInvestigador(ferramentaConsulta, ferramentaValidacao, ferramentaComparacao);
    }

    private String lerRecurso(String caminho) {
        try (InputStream is = getClass().getResourceAsStream(caminho)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deveExecutarFluxoCompletoDemonstracaoComSucesso() {
        List<MetadadosArtefato> artefatos = catalogo.listarTodos();
        assertThat(artefatos).hasSize(2);
        List<String> versoes = catalogo.listarVersoesPorComponente(ComponenteTiss.COMUNICACAO);
        assertThat(versoes).contains("04.03.00", "04.04.00");

        String xmlValido = lerRecurso("/exemplos_xml/mensagem_tiss_valida.xml");
        SolicitacaoValidacaoXmlDto reqValida = new SolicitacaoValidacaoXmlDto("lote.xml", "04.03.00", xmlValido);
        ResultadoValidacaoXmlDto resValida = ResultadoValidacaoXmlDto.de(servicoValidacao.validar(reqValida));
        assertThat(resValida.valido()).isTrue();

        String xmlInvalido = lerRecurso("/exemplos_xml/mensagem_tiss_elemento_faltante.xml");
        SolicitacaoValidacaoXmlDto reqInvalida = new SolicitacaoValidacaoXmlDto("lote_invalido.xml", "04.03.00", xmlInvalido);
        ResultadoValidacaoXmlDto resInvalida = ResultadoValidacaoXmlDto.de(servicoValidacao.validar(reqInvalida));
        assertThat(resInvalida.valido()).isFalse();
        assertThat(resInvalida.totalErros()).isGreaterThan(0);

        SolicitacaoComparacaoEsquemasDto reqComp = new SolicitacaoComparacaoEsquemasDto(
                "04.03.00",
                "tissV4_03_00.xsd",
                "04.04.00",
                "tissV4_04_00_evolucao.xsd"
        );
        ResultadoComparacaoEsquemasDto resComp = ResultadoComparacaoEsquemasDto.de(comparadorEsquemas.comparar(
                localizador.localizarCaminho(reqComp.versaoBase(), reqComp.nomeArquivoBase()),
                reqComp.versaoBase(),
                localizador.localizarCaminho(reqComp.versaoComparada(), reqComp.nomeArquivoComparado()),
                reqComp.versaoComparada()
        ));
        assertThat(resComp.esquemasIdenticos()).isFalse();
        assertThat(resComp.totalQuebrasContrato()).isGreaterThan(0);

        PerguntaInvestigacao perguntaValidacao = PerguntaInvestigacao.paraValidacao(
                "Explique por que este XML falhou",
                reqInvalida
        );
        RespostaInvestigacao respAgente = agente.investigar(perguntaValidacao);
        assertThat(respAgente.evidenciaSuficiente()).isTrue();
        assertThat(respAgente.ferramentaUtilizada()).isEqualTo("validar_xml");
        assertThat(respAgente.explicacaoFundamentada()).contains("inconsistência(s) técnica(s)");

        PerguntaInvestigacao perguntaFora = PerguntaInvestigacao.foraDeEscopo("Qual o diagnóstico clínico CID-10 do paciente?");
        RespostaInvestigacao respFora = agente.investigar(perguntaFora);
        assertThat(respFora.evidenciaSuficiente()).isFalse();
        assertThat(respFora.limitacaoOuAlerta()).contains("dados reais de pacientes");
    }
}
