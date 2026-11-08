package br.com.tissguard.api.dto;

import br.com.tissguard.comparacao.DiferencaEsquema;
import br.com.tissguard.comparacao.ResultadoComparacaoEsquema;
import br.com.tissguard.comparacao.SeveridadeDiferenca;
import br.com.tissguard.comparacao.TipoDiferencaEsquema;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import br.com.tissguard.validacao.CategoriaErroValidacao;
import br.com.tissguard.validacao.ErroValidacao;
import br.com.tissguard.validacao.ResultadoValidacao;
import br.com.tissguard.validacao.StatusValidacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ContratosApiDtoTest {

    private Validator validador;
    private ObjectMapper objectMapper;

    @BeforeEach
    void configurar() {
        validador = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void deveValidarSolicitacaoValidacaoXmlComSucessoQuandoCamposForemValidos() {
        SolicitacaoValidacaoXmlDto dto = new SolicitacaoValidacaoXmlDto(
                "guia.xml",
                "04.03.00",
                "<mensagemTISS></mensagemTISS>"
        );

        Set<ConstraintViolation<SolicitacaoValidacaoXmlDto>> violacoes = validador.validate(dto);
        assertThat(violacoes).isEmpty();
    }

    @Test
    void deveRejeitarSolicitacaoValidacaoXmlComCamposEmBranco() {
        SolicitacaoValidacaoXmlDto dtoInvalido = new SolicitacaoValidacaoXmlDto(
                "",
                "   ",
                null
        );

        Set<ConstraintViolation<SolicitacaoValidacaoXmlDto>> violacoes = validador.validate(dtoInvalido);
        assertThat(violacoes).hasSize(3);
    }

    @Test
    void deveValidarSolicitacaoComparacaoComSucessoERejeitarQuandoVersoesForemNulas() {
        SolicitacaoComparacaoEsquemasDto dtoValido = new SolicitacaoComparacaoEsquemasDto(
                "04.03.00",
                "tissV4_03_00.xsd",
                "04.04.00",
                "tissV4_04_00.xsd"
        );
        assertThat(validador.validate(dtoValido)).isEmpty();

        SolicitacaoComparacaoEsquemasDto dtoInvalido = new SolicitacaoComparacaoEsquemasDto(
                "",
                null,
                "",
                null
        );
        assertThat(validador.validate(dtoInvalido)).hasSize(2);
    }

    @Test
    void deveCriarRespostaPadraoSucessoComTimestampEMensagem() {
        RespostaPadraoApi<String> resposta = RespostaPadraoApi.sucesso("conteudo-processado", "Processado");

        assertThat(resposta.sucesso()).isTrue();
        assertThat(resposta.mensagem()).isEqualTo("Processado");
        assertThat(resposta.dados()).isEqualTo("conteudo-processado");
        assertThat(resposta.timestamp()).isNotNull();
    }

    @Test
    void deveCriarRespostaErroComDetalhes() {
        RespostaErroApi erro = RespostaErroApi.erro(
                400,
                "Bad Request",
                "Erro de validação",
                "/api/v1/validacao",
                List.of("Campo x é obrigatório", "Campo y inválido")
        );

        assertThat(erro.statusHttp()).isEqualTo(400);
        assertThat(erro.erro()).isEqualTo("Bad Request");
        assertThat(erro.mensagem()).isEqualTo("Erro de validação");
        assertThat(erro.caminho()).isEqualTo("/api/v1/validacao");
        assertThat(erro.detalhes()).hasSize(2);
        assertThat(erro.timestamp()).isNotNull();
    }

    @Test
    void deveMapearResultadoValidacaoParaDtoComFidelidade() {
        List<ErroValidacao> erros = List.of(
                new ErroValidacao(10, 5, "Elemento incorreto", CategoriaErroValidacao.ESQUEMA_XSD)
        );
        ResultadoValidacao resultadoDominio = new ResultadoValidacao(
                "guia.xml",
                "04.03.00",
                StatusValidacao.INVALIDO,
                erros,
                "Falha ao validar contra o esquema XSD"
        );

        ResultadoValidacaoXmlDto dto = ResultadoValidacaoXmlDto.de(resultadoDominio);

        assertThat(dto.nomeArquivoXml()).isEqualTo("guia.xml");
        assertThat(dto.versaoEsquema()).isEqualTo("04.03.00");
        assertThat(dto.status()).isEqualTo(StatusValidacao.INVALIDO);
        assertThat(dto.valido()).isFalse();
        assertThat(dto.totalErros()).isEqualTo(1);
        assertThat(dto.mensagemInformativa()).isEqualTo("Falha ao validar contra o esquema XSD");
        assertThat(dto.erros().getFirst().linha()).isEqualTo(10);
        assertThat(dto.erros().getFirst().coluna()).isEqualTo(5);
        assertThat(dto.erros().getFirst().categoria()).isEqualTo(CategoriaErroValidacao.ESQUEMA_XSD);
    }

    @Test
    void deveMapearResultadoComparacaoParaDtoComDiagnostico() {
        DiferencaEsquema diferenca = new DiferencaEsquema(
                TipoDiferencaEsquema.ELEMENTO_REMOVIDO,
                SeveridadeDiferenca.QUEBRA_DE_CONTRATO,
                "cabecalho/antigo",
                "Elemento removido",
                "xs:string",
                null
        );
        ResultadoComparacaoEsquema resultado = new ResultadoComparacaoEsquema(
                "v1.xsd",
                "04.03.00",
                "hash1",
                "v2.xsd",
                "04.04.00",
                "hash2",
                List.of(diferenca),
                false,
                1
        );

        ResultadoComparacaoEsquemasDto dto = ResultadoComparacaoEsquemasDto.de(resultado);

        assertThat(dto.identificadorEsquemaBase()).isEqualTo("v1.xsd");
        assertThat(dto.versaoEsquemaBase()).isEqualTo("04.03.00");
        assertThat(dto.hashEsquemaBase()).isEqualTo("hash1");
        assertThat(dto.identificadorEsquemaComparado()).isEqualTo("v2.xsd");
        assertThat(dto.versaoEsquemaComparado()).isEqualTo("04.04.00");
        assertThat(dto.hashEsquemaComparado()).isEqualTo("hash2");
        assertThat(dto.esquemasIdenticos()).isFalse();
        assertThat(dto.compativelRetroativamente()).isFalse();
        assertThat(dto.totalQuebrasContrato()).isEqualTo(1);
        assertThat(dto.diferencas()).hasSize(1);
        assertThat(dto.diferencas().getFirst().caminhoEstrutural()).isEqualTo("cabecalho/antigo");
    }

    @Test
    void deveMapearArtefatoCatalogoParaDto() {
        MetadadosArtefato artefato = new MetadadosArtefato(
                "art-1",
                "Esquema 04.03.00",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://ans.gov.br",
                LocalDate.now(),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "hash123",
                EstadoIntegridade.INTEGRO,
                "Teste"
        );

        ArtefatoCatalogoDto dto = ArtefatoCatalogoDto.de(artefato);

        assertThat(dto.id()).isEqualTo("art-1");
        assertThat(dto.componente()).isEqualTo(ComponenteTiss.COMUNICACAO);
        assertThat(dto.versaoDeclarada()).isEqualTo("04.03.00");
        assertThat(dto.hashSha256()).isEqualTo("hash123");
        assertThat(dto.estadoIntegridade()).isEqualTo(EstadoIntegridade.INTEGRO);
    }

    @Test
    void deveSerializarEDesserializarJsonComJackson() throws Exception {
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "guia.xml",
                "04.03.00",
                "<conteudo/>"
        );

        String json = objectMapper.writeValueAsString(solicitacao);
        assertThat(json).contains("guia.xml").contains("04.03.00");

        SolicitacaoValidacaoXmlDto desserializado = objectMapper.readValue(json, SolicitacaoValidacaoXmlDto.class);
        assertThat(desserializado.nomeArquivo()).isEqualTo("guia.xml");
        assertThat(desserializado.versaoEsquema()).isEqualTo("04.03.00");
        assertThat(desserializado.conteudoXml()).isEqualTo("<conteudo/>");
    }
}
