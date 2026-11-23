package br.com.tissguard.integracao;

import br.com.tissguard.agente.TipoPerguntaInvestigacao;
import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoInvestigacaoDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CriteriosAceiteMvpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String lerRecurso(String caminho) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(caminho)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void deveCumprirCriterioCatalogoInicializadoComVersoesEHashes() throws Exception {
        mockMvc.perform(get("/api/v1/artefatos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados").isArray())
                .andExpect(jsonPath("$.dados.length()").value(2));

        mockMvc.perform(get("/api/v1/artefatos/versoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0]").value("04.03.00"))
                .andExpect(jsonPath("$.dados[1]").value("04.04.00"));
    }

    @Test
    void deveCumprirCriterioValidacaoXmlValidoEInvalidoViaApi() throws Exception {
        String xmlValido = lerRecurso("/exemplos_xml/mensagem_tiss_valida.xml");
        SolicitacaoValidacaoXmlDto reqValida = new SolicitacaoValidacaoXmlDto("guia_ok.xml", "04.03.00", xmlValido);

        mockMvc.perform(post("/api/v1/validacao/xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqValida)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.valido").value(true))
                .andExpect(jsonPath("$.dados.versaoEsquema").value("04.03.00"));

        String xmlInvalido = lerRecurso("/exemplos_xml/mensagem_tiss_elemento_faltante.xml");
        SolicitacaoValidacaoXmlDto reqInvalida = new SolicitacaoValidacaoXmlDto("guia_erro.xml", "04.03.00", xmlInvalido);

        mockMvc.perform(post("/api/v1/validacao/xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalida)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.valido").value(false))
                .andExpect(jsonPath("$.dados.totalErros").isNumber());
    }

    @Test
    void deveCumprirCriterioComparacaoReproduzivelComQuebraDeContrato() throws Exception {
        SolicitacaoComparacaoEsquemasDto req = new SolicitacaoComparacaoEsquemasDto(
                "04.03.00",
                null,
                "04.04.00",
                null
        );

        mockMvc.perform(post("/api/v1/comparacao/esquemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.esquemasIdenticos").value(false))
                .andExpect(jsonPath("$.dados.totalQuebrasContrato").isNumber())
                .andExpect(jsonPath("$.dados.hashEsquemaBase").isNotEmpty())
                .andExpect(jsonPath("$.dados.hashEsquemaComparado").isNotEmpty());
    }

    @Test
    void deveCumprirCriterioAgenteDeterministicoERecusaForaDeEscopo() throws Exception {
        SolicitacaoInvestigacaoDto reqFora = new SolicitacaoInvestigacaoDto(
                TipoPerguntaInvestigacao.FORA_DE_ESCOPO,
                "Qual o valor financeiro glosado desta guia?",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/agente/investigar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqFora)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.evidenciaSuficiente").value(false))
                .andExpect(jsonPath("$.dados.ferramentaUtilizada").value("nenhuma"));
    }

    @Test
    void deveServirInterfaceDemonstracaoNaRaiz() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
    }
}
