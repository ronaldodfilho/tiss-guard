package br.com.tissguard.api;

import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.comparacao.ComparadorEsquemas;
import br.com.tissguard.comparacao.ResultadoComparacaoEsquema;
import br.com.tissguard.validacao.EsquemaNaoEncontradoException;
import br.com.tissguard.validacao.LocalizadorEsquemas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComparacaoControllerTest {

    private MockMvc mockMvc;
    private ComparadorEsquemas comparadorEsquemas;
    private LocalizadorEsquemas localizadorEsquemas;
    private ObjectMapper objectMapper;

    @BeforeEach
    void configurar() {
        comparadorEsquemas = mock(ComparadorEsquemas.class);
        localizadorEsquemas = mock(LocalizadorEsquemas.class);
        ComparacaoController controller = new ComparacaoController(comparadorEsquemas, localizadorEsquemas);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveCompararEsquemasComSucesso() throws Exception {
        Path pathBase = Path.of("v1.xsd");
        Path pathComp = Path.of("v2.xsd");
        when(localizadorEsquemas.localizarCaminho("04.03.00", null)).thenReturn(pathBase);
        when(localizadorEsquemas.localizarCaminho("04.04.00", null)).thenReturn(pathComp);

        ResultadoComparacaoEsquema resultado = new ResultadoComparacaoEsquema(
                "v1.xsd",
                "04.03.00",
                "hash1",
                "v2.xsd",
                "04.04.00",
                "hash2",
                List.of(),
                true,
                0
        );
        when(comparadorEsquemas.comparar(any(), any(), any(), any())).thenReturn(resultado);

        SolicitacaoComparacaoEsquemasDto solicitacao = new SolicitacaoComparacaoEsquemasDto(
                "04.03.00",
                null,
                "04.04.00",
                null
        );

        mockMvc.perform(post("/api/v1/comparacao/esquemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados.esquemasIdenticos").value(true));
    }

    @Test
    void deveRetornarNotFoundQuandoEsquemaNaoForLocalizado() throws Exception {
        when(localizadorEsquemas.localizarCaminho("99.99.99", null))
                .thenThrow(new EsquemaNaoEncontradoException("Esquema não encontrado"));

        SolicitacaoComparacaoEsquemasDto solicitacao = new SolicitacaoComparacaoEsquemasDto(
                "99.99.99",
                null,
                "04.04.00",
                null
        );

        mockMvc.perform(post("/api/v1/comparacao/esquemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitacao)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusHttp").value(404));
    }
}
