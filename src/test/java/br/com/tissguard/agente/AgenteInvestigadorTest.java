package br.com.tissguard.agente;

import br.com.tissguard.api.dto.ArtefatoCatalogoDto;
import br.com.tissguard.api.dto.ErroValidacaoItemDto;
import br.com.tissguard.api.dto.ResultadoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.ResultadoValidacaoXmlDto;
import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.validacao.CategoriaErroValidacao;
import br.com.tissguard.validacao.StatusValidacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgenteInvestigadorTest {

    private FerramentaConsultaArtefatos ferramentaConsulta;
    private FerramentaValidacaoXml ferramentaValidacao;
    private FerramentaComparacaoEsquemas ferramentaComparacao;
    private AgenteInvestigador agente;

    @BeforeEach
    void configurar() {
        ferramentaConsulta = mock(FerramentaConsultaArtefatos.class);
        ferramentaValidacao = mock(FerramentaValidacaoXml.class);
        ferramentaComparacao = mock(FerramentaComparacaoEsquemas.class);

        when(ferramentaValidacao.nome()).thenReturn("validar_xml");
        when(ferramentaComparacao.nome()).thenReturn("comparar_esquemas");
        when(ferramentaConsulta.nome()).thenReturn("consultar_artefatos");

        agente = new AgenteInvestigador(ferramentaConsulta, ferramentaValidacao, ferramentaComparacao);
    }

    @Test
    void deveInvestigarValidacaoComSucessoQuandoXmlForValido() {
        ResultadoValidacaoXmlDto resultadoValido = new ResultadoValidacaoXmlDto(
                "guia.xml",
                "04.03.00",
                StatusValidacao.VALIDO,
                true,
                "OK",
                List.of(),
                0
        );
        when(ferramentaValidacao.executar(any())).thenReturn(resultadoValido);

        SolicitacaoValidacaoXmlDto dados = new SolicitacaoValidacaoXmlDto("guia.xml", "04.03.00", "<xml/>");
        PerguntaInvestigacao pergunta = PerguntaInvestigacao.paraValidacao(
                "Por que este XML é válido?",
                dados
        );

        RespostaInvestigacao resposta = agente.investigar(pergunta);

        assertThat(resposta.evidenciaSuficiente()).isTrue();
        assertThat(resposta.ferramentaUtilizada()).isEqualTo("validar_xml");
        assertThat(resposta.explicacaoFundamentada()).contains("total conformidade estrutural");
        assertThat(resposta.resultadoTecnico()).isEqualTo(resultadoValido);
    }

    @Test
    void deveInvestigarValidacaoQuandoXmlPossuirErrosEVincularLinhaColuna() {
        ErroValidacaoItemDto erro = new ErroValidacaoItemDto(
                15,
                4,
                "Elemento 'ans:origem' ausente",
                CategoriaErroValidacao.ESQUEMA_XSD
        );
        ResultadoValidacaoXmlDto resultadoInvalido = new ResultadoValidacaoXmlDto(
                "guia_invalida.xml",
                "04.03.00",
                StatusValidacao.INVALIDO,
                false,
                "Falha",
                List.of(erro),
                1
        );
        when(ferramentaValidacao.executar(any())).thenReturn(resultadoInvalido);

        SolicitacaoValidacaoXmlDto dados = new SolicitacaoValidacaoXmlDto("guia_invalida.xml", "04.03.00", "<xml/>");
        PerguntaInvestigacao pergunta = PerguntaInvestigacao.paraValidacao(
                "Por que este XML falhou na validação?",
                dados
        );

        RespostaInvestigacao resposta = agente.investigar(pergunta);

        assertThat(resposta.evidenciaSuficiente()).isTrue();
        assertThat(resposta.explicacaoFundamentada())
                .contains("Linha 15")
                .contains("Coluna 4")
                .contains("ESQUEMA_XSD")
                .contains("Elemento 'ans:origem' ausente");
    }

    @Test
    void deveInvestigarComparacaoDeEsquemasEVincularHashesEDiagnostico() {
        ResultadoComparacaoEsquemasDto resultadoComp = new ResultadoComparacaoEsquemasDto(
                "tissV4_03_00.xsd",
                "04.03.00",
                "hashBaseSha256",
                "tissV4_04_00.xsd",
                "04.04.00",
                "hashCompSha256",
                false,
                false,
                2,
                1,
                0,
                "Alerta: quebra de contrato detectada.",
                "Necessário período de transição.",
                List.of(),
                3
        );
        when(ferramentaComparacao.executar(any())).thenReturn(resultadoComp);

        SolicitacaoComparacaoEsquemasDto dados = new SolicitacaoComparacaoEsquemasDto(
                "04.03.00",
                null,
                "04.04.00",
                null
        );
        PerguntaInvestigacao pergunta = PerguntaInvestigacao.paraComparacao(
                "Quais diferenças existem entre estes esquemas?",
                dados
        );

        RespostaInvestigacao resposta = agente.investigar(pergunta);

        assertThat(resposta.evidenciaSuficiente()).isTrue();
        assertThat(resposta.ferramentaUtilizada()).isEqualTo("comparar_esquemas");
        assertThat(resposta.explicacaoFundamentada())
                .contains("hashBaseSha256")
                .contains("hashCompSha256")
                .contains("quebra de contrato detectada")
                .contains("período de transição");
    }

    @Test
    void deveInvestigarConsultaDeArtefatosEVincularMetadadosEHash() {
        ArtefatoCatalogoDto artefato = new ArtefatoCatalogoDto(
                "art-1",
                "Esquema 04.03.00",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://ans.gov.br",
                LocalDate.now(),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "hashSha256Oficial",
                EstadoIntegridade.INTEGRO,
                "Obs"
        );
        when(ferramentaConsulta.executar(any())).thenReturn(List.of(artefato));

        PerguntaInvestigacao pergunta = PerguntaInvestigacao.paraConsulta(
                "Consulte os artefatos da versão 04.03.00",
                new FerramentaConsultaArtefatos.Parametros(ComponenteTiss.COMUNICACAO, "04.03.00")
        );

        RespostaInvestigacao resposta = agente.investigar(pergunta);

        assertThat(resposta.evidenciaSuficiente()).isTrue();
        assertThat(resposta.ferramentaUtilizada()).isEqualTo("consultar_artefatos");
        assertThat(resposta.explicacaoFundamentada())
                .contains("Esquema 04.03.00")
                .contains("04.03.00")
                .contains("hashSha256Oficial");
    }
}
