package br.com.tissguard.api;

import br.com.tissguard.agente.AgenteInvestigador;
import br.com.tissguard.agente.RespostaInvestigacao;
import br.com.tissguard.agente.TipoPerguntaInvestigacao;
import br.com.tissguard.api.dto.SolicitacaoInvestigacaoDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgenteControllerTest {

    private MockMvc mockMvc;
    private AgenteInvestigador agenteInvestigador;
    private ObjectMapper objectMapper;

    @BeforeEach
    void configurar() {
        agenteInvestigador = mock(AgenteInvestigador.class);
        AgenteController controller = new AgenteController(agenteInvestigador);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveInvestigarValidacaoComSucessoViaEndpoint() throws Exception {
        RespostaInvestigacao resposta = RespostaInvestigacao.comSucesso(
                "validar_xml",
                null,
                "Documento em total conformidade estrutural com o esquema XSD."
        );
        when(agenteInvestigador.investigar(any())).thenReturn(resposta);

        SolicitacaoValidacaoXmlDto dadosValidacao = new SolicitacaoValidacaoXmlDto("guia.xml", "04.03.00", "<xml/>");
        SolicitacaoInvestigacaoDto solicitacao = new SolicitacaoInvestigacaoDto(
                TipoPerguntaInvestigacao.VALIDACAO_XML,
                "Investigue a validade deste XML",
                dadosValidacao,
                null,
                null
        );

        String respostaJson = mockMvc.perform(post("/api/v1/agente/investigar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados.ferramentaUtilizada").value("validar_xml"))
                .andExpect(jsonPath("$.dados.evidenciaSuficiente").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(respostaJson).contains("total conformidade");
    }

    @Test
    void deveInvestigarForaDeEscopoERetornarSemEvidencia() throws Exception {
        RespostaInvestigacao resposta = RespostaInvestigacao.semEvidencia(
                "Solicitação fora do escopo técnico do TISS Guard.",
                "Não é permitido processamento de dados reais de pacientes."
        );
        when(agenteInvestigador.investigar(any())).thenReturn(resposta);

        SolicitacaoInvestigacaoDto solicitacao = new SolicitacaoInvestigacaoDto(
                TipoPerguntaInvestigacao.FORA_DE_ESCOPO,
                "Qual o valor da fatura hospitalar?",
                null,
                null,
                null
        );

        String respostaJson = mockMvc.perform(post("/api/v1/agente/investigar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados.evidenciaSuficiente").value(false))
                .andExpect(jsonPath("$.dados.ferramentaUtilizada").value("nenhuma"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(respostaJson)
                .contains("fora do escopo")
                .contains("dados reais de pacientes");
    }

    @Test
    void deveRetornarErro400QuandoTipoEstiverAusente() throws Exception {
        String jsonInvalido = """
                {
                    "textoPergunta": "Investigação sem tipo"
                }
                """;

        mockMvc.perform(post("/api/v1/agente/investigar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttp").value(400));
    }
}
