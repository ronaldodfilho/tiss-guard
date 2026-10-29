package br.com.tissguard.catalogo;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class CalculadorHash {

    private static final String ALGORITMO_SHA256 = "SHA-256";
    private static final int TAMANHO_BUFFER = 8192;

    public String calcularSha256(byte[] conteudo) {
        Objects.requireNonNull(conteudo, "O conteúdo em bytes não pode ser nulo");
        return calcularSha256(new ByteArrayInputStream(conteudo));
    }

    public String calcularSha256(Path caminhoArquivo) {
        Objects.requireNonNull(caminhoArquivo, "O caminho do arquivo não pode ser nulo");

        if (!Files.exists(caminhoArquivo)) {
            throw new FalhaCalculoHashException("Arquivo não encontrado para cálculo de hash: " + caminhoArquivo);
        }

        if (Files.isDirectory(caminhoArquivo)) {
            throw new FalhaCalculoHashException("O caminho informado é um diretório e não um arquivo: " + caminhoArquivo);
        }

        try (InputStream stream = Files.newInputStream(caminhoArquivo)) {
            return calcularSha256(stream);
        } catch (IOException e) {
            throw new FalhaCalculoHashException("Falha ao ler o arquivo para cálculo de hash: " + caminhoArquivo, e);
        }
    }

    public String calcularSha256(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "O fluxo de entrada não pode ser nulo");

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_SHA256);
            byte[] buffer = new byte[TAMANHO_BUFFER];
            int bytesLidos;

            while ((bytesLidos = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesLidos);
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash SHA-256 não disponível no ambiente Java", e);
        } catch (IOException e) {
            throw new FalhaCalculoHashException("Erro de leitura durante o cálculo de hash SHA-256", e);
        }
    }
}
