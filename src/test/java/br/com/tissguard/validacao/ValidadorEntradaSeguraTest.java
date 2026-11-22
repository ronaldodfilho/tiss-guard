package br.com.tissguard.validacao;

import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.config.PropriedadesTissGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorEntradaSeguraTest {

    private PropriedadesTissGuard propriedades;
    private ValidadorEntradaSegura validador;

    @BeforeEach
    void configurar() {
        propriedades = new PropriedadesTissGuard();
        propriedades.setTamanhoMaximoUploadBytes(1024);
        validador = new ValidadorEntradaSegura(propriedades);
    }

    @Test
    void deveAceitarSolicitacaoValidaESegura() {
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "guia_sintetica_01.xml",
                "04.03.00",
                "<xml>conteudo valido</xml>"
        );

        assertThatCode(() -> validador.validar(solicitacao)).doesNotThrowAnyException();
    }

    @Test
    void deveBloquearTentativaDePathTraversalNoNomeArquivo() {
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "../../etc/passwd.xml",
                "04.03.00",
                "<xml/>"
        );

        assertThatThrownBy(() -> validador.validar(solicitacao))
                .isInstanceOf(XmlSegurancaException.class)
                .hasMessageContaining("travessia de diretório");
    }

    @Test
    void deveBloquearExtensaoNaoXml() {
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "script.exe",
                "04.03.00",
                "<xml/>"
        );

        assertThatThrownBy(() -> validador.validar(solicitacao))
                .isInstanceOf(XmlSegurancaException.class)
                .hasMessageContaining("extensão .xml");
    }

    @Test
    void deveBloquearPayloadAcimaDoLimiteConfigurado() {
        String xmlGrande = "<a>" + "x".repeat(2000) + "</a>";
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "arquivo_grande.xml",
                "04.03.00",
                xmlGrande
        );

        assertThatThrownBy(() -> validador.validar(solicitacao))
                .isInstanceOf(XmlSegurancaException.class)
                .hasMessageContaining("excede o limite máximo permitido");
    }

    @Test
    void deveBloquearVersaoComCaracteresPerigosos() {
        SolicitacaoValidacaoXmlDto solicitacao = new SolicitacaoValidacaoXmlDto(
                "guia.xml",
                "../04.03.00",
                "<xml/>"
        );

        assertThatThrownBy(() -> validador.validar(solicitacao))
                .isInstanceOf(XmlSegurancaException.class)
                .hasMessageContaining("caracteres não permitidos");
    }
}
