package br.com.tissguard.validacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CasosDeBordaValidacaoTest {

    private ValidadorXml validador;
    private LeitorXmlSeguro leitor;

    @BeforeEach
    void configurar() {
        leitor = new LeitorXmlSeguro(1024 * 1024);
        validador = new ValidadorXml(leitor);
    }

    private StreamSource obterEsquemaPadrao() {
        InputStream stream = getClass().getResourceAsStream("/esquemas_xsd/tissV4_03_00.xsd");
        return new StreamSource(stream);
    }

    @Test
    void deveRejeitarXmlApenasComEspacosEmBranco() {
        assertThatThrownBy(() -> validador.validar("vazio.xml", "   \n\t  ", "04.03.00", obterEsquemaPadrao()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveTratarXmlComAcentuacaoECaracteresEspeciaisCorretamente() {
        String xmlComAcentos = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ans:mensagemTISS xmlns:ans="http://www.ans.gov.br/padroes/tiss/schemas">
                    <ans:cabecalho>
                        <ans:identificacaoTransacao>
                            <ans:tipoTransacao>SOLICITAÇÃO_DE_PROCEDIMENTOS</ans:tipoTransacao>
                            <ans:sequencialTransacao>123456</ans:sequencialTransacao>
                            <ans:dataRegistroTransacao>2024-04-01</ans:dataRegistroTransacao>
                            <ans:horaRegistroTransacao>14:30:00</ans:horaRegistroTransacao>
                        </ans:identificacaoTransacao>
                        <ans:origem>
                            <ans:identificacaoPrestador>
                                <ans:codigoPrestadorNaOperadora>PRESTADOR_SÃO_PAULO</ans:codigoPrestadorNaOperadora>
                            </ans:identificacaoPrestador>
                        </ans:origem>
                        <ans:destino>
                            <ans:registroANS>123456</ans:registroANS>
                        </ans:destino>
                        <ans:versaoPadrao>04.03.00</ans:versaoPadrao>
                    </ans:cabecalho>
                </ans:mensagemTISS>
                """;

        ResultadoValidacao resultado = validador.validar(
                "acentuado.xml",
                xmlComAcentos.getBytes(StandardCharsets.UTF_8),
                "04.03.00",
                obterEsquemaPadrao()
        );

        assertThat(resultado.isValido()).isTrue();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.VALIDO);
    }

    @Test
    void deveInterromperProcessamentoQuandoPayloadExcederLimiteConfigurado() {
        LeitorXmlSeguro leitorLimitePequeno = new LeitorXmlSeguro(100);
        ValidadorXml validadorLimite = new ValidadorXml(leitorLimitePequeno);

        byte[] payloadGrande = new byte[200];
        for (int i = 0; i < payloadGrande.length; i++) {
            payloadGrande[i] = 'a';
        }

        ResultadoValidacao resultado = validadorLimite.validar(
                "gigante.xml",
                payloadGrande,
                "04.03.00",
                obterEsquemaPadrao()
        );

        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.NAO_CONCLUIDO);
        assertThat(resultado.erros().getFirst().categoria()).isEqualTo(CategoriaErroValidacao.SEGURANCA);
        assertThat(resultado.erros().getFirst().mensagem()).contains("excede o limite");
    }

    @Test
    void deveRetornarErroNaoConcluidoQuandoXmlPossuirApenasComentario() {
        String xmlSoComentario = "<!-- apenas comentário -->";

        ResultadoValidacao resultado = validador.validar(
                "comentario.xml",
                xmlSoComentario,
                "04.03.00",
                obterEsquemaPadrao()
        );

        assertThat(resultado.isValido()).isFalse();
        assertThat(resultado.status()).isEqualTo(StatusValidacao.NAO_CONCLUIDO);
        assertThat(resultado.temErroSintaxe()).isTrue();
    }
}
