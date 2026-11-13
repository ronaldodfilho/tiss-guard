package br.com.tissguard.validacao;

import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacaoIntegracaoTest {

    private ServicoValidacao servicoValidacao;

    @BeforeEach
    void configurar() {
        LocalizadorEsquemas localizador = new LocalizadorEsquemas();
        ValidadorXml validador = new ValidadorXml();
        servicoValidacao = new ServicoValidacao(validador, localizador);
    }

    private String lerRecurso(String caminho) {
        try (InputStream is = getClass().getResourceAsStream(caminho)) {
            if (is == null) {
                throw new IllegalArgumentException("Recurso não encontrado: " + caminho);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler recurso de teste: " + caminho, e);
        }
    }

    @Test
    void deveValidarXmlValidoComSucessoNoFluxoIntegrado() {
        String xml = lerRecurso("/exemplos_xml/mensagem_tiss_valida.xml");
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "mensagem_tiss_valida.xml",
                "04.03.00",
                xml
        );

        ResultadoValidacao resultado = servicoValidacao.validar(solicitacao);

        assertThat(resultado.isValido()).isTrue();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.VALIDO);
        assertThat(resultado.erros()).isEmpty();
    }

    @Test
    void deveDetectarInconsistenciasEmXmlComElementoFaltante() {
        String xml = lerRecurso("/exemplos_xml/mensagem_tiss_elemento_faltante.xml");
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "mensagem_tiss_elemento_faltante.xml",
                "04.03.00",
                xml
        );

        ResultadoValidacao resultado = servicoValidacao.validar(solicitacao);

        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.INVALIDO);
        assertThat(resultado.erros()).isNotEmpty();
        assertThat(resultado.temErroEsquema()).isTrue();
    }

    @Test
    void deveBloquearXmlComTentativaDeXxeNoFluxoIntegrado() {
        String xml = lerRecurso("/exemplos_xml/xml_com_xxe.xml");
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "xml_com_xxe.xml",
                "04.03.00",
                xml
        );

        ResultadoValidacao resultado = servicoValidacao.validar(solicitacao);

        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.NAO_CONCLUIDO);
        assertThat(resultado.erros().getFirst().categoria()).isEqualTo(CategoriaErroValidacao.SEGURANCA);
    }

    @Test
    void deveLancarExcecaoQuandoEsquemaForInexistente() {
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "teste.xml",
                "99.88.77",
                "<tag/>"
        );

        assertThatThrownBy(() -> servicoValidacao.validar(solicitacao))
                .isInstanceOf(EsquemaNaoEncontradoException.class);
    }
}
