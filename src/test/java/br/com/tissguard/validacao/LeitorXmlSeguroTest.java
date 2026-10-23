package br.com.tissguard.validacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeitorXmlSeguroTest {

    private LeitorXmlSeguro leitor;

    @BeforeEach
    void configurar() {
        leitor = new LeitorXmlSeguro();
    }

    private byte[] carregarRecurso(String caminho) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (stream == null) {
                throw new IllegalArgumentException("Recurso não encontrado: " + caminho);
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler recurso de teste: " + caminho, e);
        }
    }

    @Test
    void deveLerXmlValidoComSucesso() {
        byte[] bytes = carregarRecurso("exemplos_xml/mensagem_tiss_valida.xml");

        DocumentoXml documento = leitor.ler("mensagem_tiss_valida.xml", bytes);

        assertThat(documento).isNotNull();
        assertThat(documento.nomeArquivo()).isEqualTo("mensagem_tiss_valida.xml");
        assertThat(documento.elementoRaiz()).isEqualTo("ans:mensagemTISS");
        assertThat(documento.tamanhoBytes()).isPositive();
        assertThat(documento.documento()).isNotNull();
    }

    @Test
    void deveLerXmlValidoPassadoComoString() {
        String xml = "<ans:mensagemTISS xmlns:ans=\"http://www.ans.gov.br/padroes/tiss/schemas\"><ans:conteudo/></ans:mensagemTISS>";

        DocumentoXml documento = leitor.ler("simples.xml", xml);

        assertThat(documento.elementoRaiz()).isEqualTo("ans:mensagemTISS");
        assertThat(documento.tamanhoBytes()).isEqualTo(xml.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void deveLancarExcecaoQuandoXmlForMalformado() {
        byte[] bytes = carregarRecurso("exemplos_xml/xml_malformado.xml");

        assertThatThrownBy(() -> leitor.ler("xml_malformado.xml", bytes))
                .isInstanceOf(XmlMalformadoException.class)
                .hasMessageContaining("malformado");
    }

    @Test
    void deveBloquearXmlComDoctypeOuEntidadeExterna() {
        byte[] bytes = carregarRecurso("exemplos_xml/xml_com_xxe.xml");

        assertThatThrownBy(() -> leitor.ler("xml_com_xxe.xml", bytes))
                .isInstanceOf(XmlSegurancaException.class)
                .hasMessageContaining("DOCTYPE");
    }

    @Test
    void deveBloquearArquivoQuandoExcederLimiteMaximo() {
        LeitorXmlSeguro leitorRestrito = new LeitorXmlSeguro(50);
        byte[] bytes = carregarRecurso("exemplos_xml/mensagem_tiss_valida.xml");

        assertThatThrownBy(() -> leitorRestrito.ler("mensagem_tiss_valida.xml", bytes))
                .isInstanceOf(XmlLimiteExcedidoException.class)
                .hasMessageContaining("excede o limite máximo permitido");
    }

    @Test
    void deveLancarExcecaoQuandoNomeArquivoOuConteudoForemInvalidos() {
        assertThatThrownBy(() -> leitor.ler("", "<xml/>"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> leitor.ler("arquivo.xml", ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> leitor.ler("arquivo.xml", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> leitor.ler(null, "<xml/>"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> leitor.ler("arquivo.xml", (byte[]) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveLancarExcecaoQuandoLimiteMaximoForInvalido() {
        assertThatThrownBy(() -> new LeitorXmlSeguro(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LeitorXmlSeguro(-10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
