package br.com.tissguard.fontes;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteObtencaoArtefatoTest {

    private HttpServer servidorLocal;
    private String urlBaseServidor;
    private ClienteObtencaoArtefato cliente;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidorLocal = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorLocal.start();
        int porta = servidorLocal.getAddress().getPort();
        urlBaseServidor = "http://127.0.0.1:" + porta;
        cliente = new ClienteObtencaoArtefato();
    }

    @AfterEach
    void pararServidor() {
        if (servidorLocal != null) {
            servidorLocal.stop(0);
        }
    }

    @Test
    void deveObterArtefatoComSucessoQuandoRespostaForValida() {
        String conteudoXsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"></xs:schema>";
        servidorLocal.createContext("/tissV4_03_00.xsd", exchange -> {
            byte[] bytes = conteudoXsd.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        URI uri = URI.create(urlBaseServidor + "/tissV4_03_00.xsd");
        ResultadoObtencaoArtefato resultado = cliente.obter(uri, "tissV4_03_00.xsd");

        assertThat(resultado).isNotNull();
        assertThat(resultado.nomeArquivo()).isEqualTo("tissV4_03_00.xsd");
        assertThat(resultado.codigoStatusHttp()).isEqualTo(200);
        assertThat(resultado.tamanhoBytes()).isEqualTo(conteudoXsd.getBytes(StandardCharsets.UTF_8).length);
        assertThat(new String(resultado.conteudo(), StandardCharsets.UTF_8)).isEqualTo(conteudoXsd);
    }

    @Test
    void deveLancarExcecaoQuandoServidorRetornarCodigoHttpDeErro() {
        servidorLocal.createContext("/nao-encontrado.xsd", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        URI uri = URI.create(urlBaseServidor + "/nao-encontrado.xsd");

        assertThatThrownBy(() -> cliente.obter(uri, "nao-encontrado.xsd"))
                .isInstanceOf(FalhaObtencaoArtefatoException.class)
                .hasMessageContaining("Falha HTTP 404");
    }

    @Test
    void deveLancarExcecaoQuandoServidorRetornarErroInterno500() {
        servidorLocal.createContext("/erro-interno.xsd", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        URI uri = URI.create(urlBaseServidor + "/erro-interno.xsd");

        assertThatThrownBy(() -> cliente.obter(uri, "erro-interno.xsd"))
                .isInstanceOf(FalhaObtencaoArtefatoException.class)
                .hasMessageContaining("Falha HTTP 500");
    }

    @Test
    void deveLancarExcecaoQuandoArquivoRetornadoEstiverVazio() {
        servidorLocal.createContext("/arquivo-vazio.xsd", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });

        URI uri = URI.create(urlBaseServidor + "/arquivo-vazio.xsd");

        assertThatThrownBy(() -> cliente.obter(uri, "arquivo-vazio.xsd"))
                .isInstanceOf(FalhaObtencaoArtefatoException.class)
                .hasMessageContaining("está vazio (0 bytes)");
    }

    @Test
    void deveLancarExcecaoQuandoConteudoForHtmlInesperadoAoInvesDeXsd() {
        String htmlErro = "<!DOCTYPE html><html><head><title>Erro no Portal</title></head><body>Acesso Negado</body></html>";
        servidorLocal.createContext("/pagina-erro.xsd", exchange -> {
            byte[] bytes = htmlErro.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        URI uri = URI.create(urlBaseServidor + "/pagina-erro.xsd");

        assertThatThrownBy(() -> cliente.obter(uri, "tissV4_03_00.xsd"))
                .isInstanceOf(FalhaObtencaoArtefatoException.class)
                .hasMessageContaining("Conteúdo inesperado")
                .hasMessageContaining("HTML");
    }

    @Test
    void deveLancarExcecaoQuandoOcorrerTimeoutDeRequisicao() {
        servidorLocal.createContext("/lento.xsd", exchange -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = "<xs:schema/>".getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException ignored) {
            }
        });

        HttpClient httpClient = HttpClient.newBuilder().build();
        ClienteObtencaoArtefato clienteComTimeoutCurto = new ClienteObtencaoArtefato(httpClient, Duration.ofMillis(50));
        URI uri = URI.create(urlBaseServidor + "/lento.xsd");

        assertThatThrownBy(() -> clienteComTimeoutCurto.obter(uri, "lento.xsd"))
                .isInstanceOf(FalhaObtencaoArtefatoException.class)
                .hasMessageContaining("Tempo limite excedido");
    }

    @Test
    void deveValidarParametrosObrigatorios() {
        URI uriValida = URI.create(urlBaseServidor + "/teste.xsd");

        assertThatThrownBy(() -> cliente.obter(null, "teste.xsd"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cliente.obter(uriValida, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cliente.obter(uriValida, "   "))
                .isInstanceOf(IllegalArgumentException.class);

        URI uriFtp = URI.create("ftp://servidor/arquivo.xsd");
        assertThatThrownBy(() -> cliente.obter(uriFtp, "arquivo.xsd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protocolo HTTP ou HTTPS");
    }
}
