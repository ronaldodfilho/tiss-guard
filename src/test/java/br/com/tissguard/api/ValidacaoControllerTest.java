package br.com.tissguard.api;

import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.validacao.ResultadoValidacao;
import br.com.tissguard.validacao.ServicoValidacao;
import br.com.tissguard.validacao.StatusValidacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidacaoControllerTest {

    private MockMvc mockMvc;
    private ServicoValidacao servicoValidacao;
    private ObjectMapper objectMapper;

    @BeforeEach
    void configurar() {
        servicoValidacao = mock(ServicoValidacao.class);
        ValidacaoController controller = new ValidacaoController(servicoValidacao);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveRetornarSucessoQuandoXmlForValido() throws Exception {
        ResultadoValidacao resultado = new ResultadoValidacao(
                "guia.xml",
                "04.03.00",
                StatusValidacao.VALIDO,
                List.of(),
                "Validação concluída com sucesso."
        );
        when(servicoValidacao.validar(any())).thenReturn(resultado);

        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "guia.xml",
                "04.03.00",
                "<mensagemTISS></mensagemTISS>"
        );

        mockMvc.perform(post("/api/v1/validacao/xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados.valido").value(true))
                .andExpect(jsonPath("$.dados.nomeArquivoXml").value("guia.xml"))
                .andExpect(jsonPath("$.dados.versaoEsquema").value("04.03.00"));
    }

    @Test
    void deveRetornarBadRequestQuandoParametrosForemInvalidos() throws Exception {
        SolicitacaoValidacaoXmlDto solicitacaoInvalida = new SolicitacaoValidacaoXmlDto(
                "",
                "04.03.00",
                ""
        );

        mockMvc.perform(post("/api/v1/validacao/xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitacaoInvalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttp").value(400))
                .andExpect(jsonPath("$.detalhes").isArray());
    }
}
