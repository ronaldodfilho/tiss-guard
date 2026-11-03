package br.com.tissguard.fontes;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@Component
public class ClienteObtencaoArtefato {

    private static final Duration TIMEOUT_CONEXAO_PADRAO = Duration.ofSeconds(10);
    private static final Duration TIMEOUT_REQUISICAO_PADRAO = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final Duration timeoutRequisicao;

    public ClienteObtencaoArtefato() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(TIMEOUT_CONEXAO_PADRAO)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                TIMEOUT_REQUISICAO_PADRAO
        );
    }

    public ClienteObtencaoArtefato(HttpClient httpClient, Duration timeoutRequisicao) {
        this.httpClient = Objects.requireNonNull(httpClient, "O cliente HTTP não pode ser nulo");
        this.timeoutRequisicao = Objects.requireNonNull(timeoutRequisicao, "O timeout de requisição não pode ser nulo");
    }

    public ResultadoObtencaoArtefato obter(URI uriOrigem, String nomeArquivoEsperado) {
        Objects.requireNonNull(uriOrigem, "A URI de origem não pode ser nula");
        Objects.requireNonNull(nomeArquivoEsperado, "O nome do arquivo esperado não pode ser nulo");

        if (nomeArquivoEsperado.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo esperado não pode estar em branco");
        }

        String esquemaUri = uriOrigem.getScheme();
        if (esquemaUri == null || (!esquemaUri.equalsIgnoreCase("http") && !esquemaUri.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("A URI deve utilizar protocolo HTTP ou HTTPS: " + uriOrigem);
        }

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(uriOrigem)
                .timeout(timeoutRequisicao)
                .header("Accept", "application/xml, text/xml, */*")
                .header("User-Agent", "TISS-Guard-Monitor/1.0")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofByteArray());
            int codigoStatus = resposta.statusCode();

            if (codigoStatus < 200 || codigoStatus >= 300) {
                throw new FalhaObtencaoArtefatoException(
                        "Falha HTTP " + codigoStatus + " ao obter artefato da fonte oficial: " + uriOrigem
                );
            }

            byte[] corpo = resposta.body();
            if (corpo == null || corpo.length == 0) {
                throw new FalhaObtencaoArtefatoException(
                        "O arquivo retornado pela fonte oficial está vazio (0 bytes): " + uriOrigem
                );
            }

            validarConteudoEsperado(nomeArquivoEsperado, corpo, uriOrigem);

            String tipoConteudo = resposta.headers().firstValue("Content-Type").orElse("application/octet-stream");

            return new ResultadoObtencaoArtefato(
                    nomeArquivoEsperado,
                    corpo,
                    codigoStatus,
                    tipoConteudo,
                    corpo.length
            );
        } catch (HttpTimeoutException e) {
            throw new FalhaObtencaoArtefatoException(
                    "Tempo limite excedido ao tentar obter artefato da fonte oficial: " + uriOrigem, e
            );
        } catch (IOException e) {
            throw new FalhaObtencaoArtefatoException(
                    "Falha de comunicação de rede ao acessar fonte oficial: " + uriOrigem + ". Detalhe: " + e.getMessage(), e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaObtencaoArtefatoException(
                    "Operação de obtenção do artefato interrompida: " + uriOrigem, e
            );
        }
    }

    private void validarConteudoEsperado(String nomeArquivo, byte[] corpo, URI uriOrigem) {
        String nomeMinusculo = nomeArquivo.toLowerCase();
        if (nomeMinusculo.endsWith(".xsd") || nomeMinusculo.endsWith(".xml")) {
            int tamanhoAmostra = Math.min(corpo.length, 512);
            String inicioTexto = new String(corpo, 0, tamanhoAmostra, StandardCharsets.UTF_8).trim().toLowerCase();

            if (inicioTexto.startsWith("<!doctype html") || inicioTexto.startsWith("<html")) {
                throw new FalhaObtencaoArtefatoException(
                        "Conteúdo inesperado retornado pela fonte oficial para o arquivo " + nomeArquivo +
                        ". Esperava-se esquema ou documento XML, mas foi recebido documento HTML: " + uriOrigem
                );
            }
        }
    }
}
