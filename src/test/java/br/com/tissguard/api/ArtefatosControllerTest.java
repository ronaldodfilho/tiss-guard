package br.com.tissguard.api;

import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArtefatosControllerTest {

    private MockMvc mockMvc;
    private CatalogoArtefatos catalogoArtefatos;

    @BeforeEach
    void configurar() {
        catalogoArtefatos = mock(CatalogoArtefatos.class);
        ArtefatosController controller = new ArtefatosController(catalogoArtefatos);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveListarArtefatos() throws Exception {
        MetadadosArtefato artefato = new MetadadosArtefato(
                "art-1",
                "Esquema Principal",
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
        when(catalogoArtefatos.listarTodos()).thenReturn(List.of(artefato));

        mockMvc.perform(get("/api/v1/artefatos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados[0].id").value("art-1"))
                .andExpect(jsonPath("$.dados[0].versaoDeclarada").value("04.03.00"));
    }

    @Test
    void deveRetornarNotFoundQuandoArtefatoNaoExistir() throws Exception {
        when(catalogoArtefatos.buscarPorId("inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/artefatos/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusHttp").value(404));
    }

    @Test
    void deveListarVersoesPorComponente() throws Exception {
        when(catalogoArtefatos.listarVersoesPorComponente(ComponenteTiss.COMUNICACAO))
                .thenReturn(List.of("04.03.00", "04.04.00"));

        mockMvc.perform(get("/api/v1/artefatos/versoes?componente=COMUNICACAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0]").value("04.03.00"))
                .andExpect(jsonPath("$.dados[1]").value("04.04.00"));
    }
}
