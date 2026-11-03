package br.com.tissguard.fontes;

import br.com.tissguard.catalogo.CalculadorHash;
import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.catalogo.DetectorAlteracoesArtefato;
import br.com.tissguard.catalogo.TipoAlteracaoArtefato;
import br.com.tissguard.dominio.ComponenteTiss;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoObtencaoArtefatosTest {

    private HttpServer servidorLocal;
    private String urlBaseServidor;
    private CatalogoArtefatos catalogo;
    private CalculadorHash calculadorHash;
    private DetectorAlteracoesArtefato detector;
    private ClienteObtencaoArtefato cliente;
    private ServicoObtencaoArtefatos servico;

    @TempDir
    Path diretorioTemporario;

    @BeforeEach
    void iniciarAmbiente() throws IOException {
        servidorLocal = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorLocal.start();
        int porta = servidorLocal.getAddress().getPort();
        urlBaseServidor = "http://127.0.0.1:" + porta;

        catalogo = new CatalogoArtefatos();
        calculadorHash = new CalculadorHash();
        detector = new DetectorAlteracoesArtefato(catalogo);
        cliente = new ClienteObtencaoArtefato();
        servico = new ServicoObtencaoArtefatos(cliente, calculadorHash, detector, catalogo);
    }

    @AfterEach
    void pararAmbiente() {
        if (servidorLocal != null) {
            servidorLocal.stop(0);
        }
    }

    private void registrarEndpointServidor(String caminho, String conteudo, String contentType, int status) {
        servidorLocal.createContext(caminho, exchange -> {
            byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void deveObterNovoArtefatoESalvarNoArmazenamentoLocal() {
        String conteudoXsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"mensagemTISS\"/></xs:schema>";
        registrarEndpointServidor("/tissV4_03_00.xsd", conteudoXsd, "application/xml", 200);

        SolicitacaoObtencaoArtefato solicitacao = new SolicitacaoObtencaoArtefato(
                "comunicacao-v4-03-00-principal",
                "Esquema Principal TISS v04.03.00",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                URI.create(urlBaseServidor + "/tissV4_03_00.xsd"),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Esquema oficial baixado"
        );

        ResultadoProcessamentoObtencao resultado = servico.processar(solicitacao, diretorioTemporario);

        assertThat(resultado).isNotNull();
        assertThat(resultado.deteccao().tipo()).isEqualTo(TipoAlteracaoArtefato.NOVO_ARTEFATO);
        assertThat(resultado.arquivoGravado()).isTrue();
        assertThat(resultado.caminhoArquivoSalvo()).exists();
        assertThat(resultado.metadados().hashSha256()).isEqualTo(calculadorHash.calcularSha256(conteudoXsd.getBytes(StandardCharsets.UTF_8)));

        assertThat(catalogo.quantidadeArtefatos()).isEqualTo(1);
        assertThat(catalogo.buscarPorId("comunicacao-v4-03-00-principal")).isPresent();
    }

    @Test
    void deveSuportarCoexistenciaDeMultiplasVersoesSemSobrescrita() {
        String xsdVersao1 = "<xs:schema versao=\"04.03.00\"/>";
        String xsdVersao2 = "<xs:schema versao=\"04.04.00\"/>";

        registrarEndpointServidor("/v4_03_00.xsd", xsdVersao1, "application/xml", 200);
        registrarEndpointServidor("/v4_04_00.xsd", xsdVersao2, "application/xml", 200);

        SolicitacaoObtencaoArtefato v1 = new SolicitacaoObtencaoArtefato(
                "art-v1",
                "Esquema v1",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                URI.create(urlBaseServidor + "/v4_03_00.xsd"),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Versao 04.03.00"
        );

        SolicitacaoObtencaoArtefato v2 = new SolicitacaoObtencaoArtefato(
                "art-v2",
                "Esquema v2",
                ComponenteTiss.COMUNICACAO,
                "04.04.00",
                URI.create(urlBaseServidor + "/v4_04_00.xsd"),
                LocalDate.of(2024, 10, 1),
                "tissV4_04_00.xsd",
                "Versao 04.04.00"
        );

        ResultadoProcessamentoObtencao resultadoV1 = servico.processar(v1, diretorioTemporario);
        ResultadoProcessamentoObtencao resultadoV2 = servico.processar(v2, diretorioTemporario);

        assertThat(resultadoV1.deteccao().tipo()).isEqualTo(TipoAlteracaoArtefato.NOVO_ARTEFATO);
        assertThat(resultadoV2.deteccao().tipo()).isEqualTo(TipoAlteracaoArtefato.NOVA_VERSAO);

        assertThat(resultadoV1.caminhoArquivoSalvo()).exists();
        assertThat(resultadoV2.caminhoArquivoSalvo()).exists();
        assertThat(resultadoV1.caminhoArquivoSalvo()).isNotEqualTo(resultadoV2.caminhoArquivoSalvo());

        assertThat(catalogo.quantidadeArtefatos()).isEqualTo(2);
        assertThat(catalogo.listarVersoesPorComponente(ComponenteTiss.COMUNICACAO)).containsExactly("04.03.00", "04.04.00");
    }

    @Test
    void deveIgnorarGravacaoFisicaQuandoArtefatoForIdenticoAoCadastrado() {
        String conteudoXsd = "<xs:schema versao=\"04.03.00\"/>";
        registrarEndpointServidor("/tissV4_03_00.xsd", conteudoXsd, "application/xml", 200);

        SolicitacaoObtencaoArtefato solicitacao = new SolicitacaoObtencaoArtefato(
                "art-1",
                "Esquema Original",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                URI.create(urlBaseServidor + "/tissV4_03_00.xsd"),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Download inicial"
        );

        ResultadoProcessamentoObtencao resultadoInicial = servico.processar(solicitacao, diretorioTemporario);
        assertThat(resultadoInicial.arquivoGravado()).isTrue();

        ResultadoProcessamentoObtencao resultadoRepetido = servico.processar(solicitacao, diretorioTemporario);
        assertThat(resultadoRepetido.deteccao().tipo()).isEqualTo(TipoAlteracaoArtefato.SEM_ALTERACAO);
        assertThat(resultadoRepetido.arquivoGravado()).isFalse();
        assertThat(catalogo.quantidadeArtefatos()).isEqualTo(1);
    }

    @Test
    void deveTratarModificacaoSilenciosaSalvandoEmAreaDeAlertaSemCorromperHistorico() {
        String xsdOriginal = "<xs:schema versao=\"04.03.00\" original=\"true\"/>";
        String xsdModificado = "<xs:schema versao=\"04.03.00\" original=\"modificado_silenciosamente\"/>";

        registrarEndpointServidor("/original.xsd", xsdOriginal, "application/xml", 200);
        registrarEndpointServidor("/modificado.xsd", xsdModificado, "application/xml", 200);

        SolicitacaoObtencaoArtefato original = new SolicitacaoObtencaoArtefato(
                "art-1",
                "Esquema 04.03.00",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                URI.create(urlBaseServidor + "/original.xsd"),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Download inicial confiavel"
        );

        servico.processar(original, diretorioTemporario);

        SolicitacaoObtencaoArtefato modificado = new SolicitacaoObtencaoArtefato(
                "art-2",
                "Esquema 04.03.00 Modificado",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                URI.create(urlBaseServidor + "/modificado.xsd"),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Possivel alteracao silenciosa da ANS"
        );

        ResultadoProcessamentoObtencao resultadoAlerta = servico.processar(modificado, diretorioTemporario);

        assertThat(resultadoAlerta.deteccao().tipo()).isEqualTo(TipoAlteracaoArtefato.CONTEUDO_MODIFICADO);
        assertThat(resultadoAlerta.deteccao().exigeAtencao()).isTrue();
        assertThat(resultadoAlerta.caminhoArquivoSalvo().toString()).contains("alertas_modificacao");
        assertThat(resultadoAlerta.caminhoArquivoSalvo()).exists();

        Path caminhoOriginal = diretorioTemporario.resolve("versoes").resolve("04.03.00").resolve("tissV4_03_00.xsd");
        assertThat(caminhoOriginal).exists();
        assertThat(calculadorHash.calcularSha256(caminhoOriginal)).isEqualTo(calculadorHash.calcularSha256(xsdOriginal.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void devePropagarExcecaoQuandoDownloadFalhar() {
        registrarEndpointServidor("/erro-404.xsd", "", "text/plain", 404);

        SolicitacaoObtencaoArtefato solicitacao = new SolicitacaoObtencaoArtefato(
                "art-falha",
                "Esquema Inexistente",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                URI.create(urlBaseServidor + "/erro-404.xsd"),
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Falha esperada"
        );

        assertThatThrownBy(() -> servico.processar(solicitacao, diretorioTemporario))
                .isInstanceOf(FalhaObtencaoArtefatoException.class)
                .hasMessageContaining("Falha HTTP 404");
    }
}
