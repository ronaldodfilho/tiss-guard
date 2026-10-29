package br.com.tissguard.catalogo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadorHashTest {

    private CalculadorHash calculador;

    @BeforeEach
    void configurar() {
        calculador = new CalculadorHash();
    }

    @Test
    void deveCalcularHashSha256CorretoAPartirDeBytes() {
        byte[] conteudo = "teste".getBytes(StandardCharsets.UTF_8);
        String hashEsperado = "46070d4bf934fb0d4b06d9e2c46e346944e322444900a435d7d9a95e6d7435f5";

        String hashCalculado = calculador.calcularSha256(conteudo);

        assertThat(hashCalculado).isEqualTo(hashEsperado);
    }

    @Test
    void deveCalcularHashSha256DeConteudoVazio() {
        byte[] conteudoVazio = new byte[0];
        String hashVazioSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        String hashCalculado = calculador.calcularSha256(conteudoVazio);

        assertThat(hashCalculado).isEqualTo(hashVazioSha256);
    }

    @Test
    void deveCalcularHashSha256APartirDeInputStream() {
        byte[] conteudo = "teste".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream stream = new ByteArrayInputStream(conteudo);

        String hashCalculado = calculador.calcularSha256(stream);

        assertThat(hashCalculado).isEqualTo("46070d4bf934fb0d4b06d9e2c46e346944e322444900a435d7d9a95e6d7435f5");
    }

    @Test
    void deveCalcularHashSha256DeArquivoEmDisco(@TempDir Path diretorioTemporario) throws IOException {
        Path arquivo = diretorioTemporario.resolve("arquivo_teste.txt");
        Files.writeString(arquivo, "teste", StandardCharsets.UTF_8);

        String hashCalculado = calculador.calcularSha256(arquivo);

        assertThat(hashCalculado).isEqualTo("46070d4bf934fb0d4b06d9e2c46e346944e322444900a435d7d9a95e6d7435f5");
    }

    @Test
    void deveLancarExcecaoQuandoArquivoNaoExistir(@TempDir Path diretorioTemporario) {
        Path arquivoInexistente = diretorioTemporario.resolve("nao_existe.txt");

        assertThatThrownBy(() -> calculador.calcularSha256(arquivoInexistente))
                .isInstanceOf(FalhaCalculoHashException.class)
                .hasMessageContaining("Arquivo não encontrado");
    }

    @Test
    void deveLancarExcecaoQuandoCaminhoForDiretorio(@TempDir Path diretorioTemporario) {
        assertThatThrownBy(() -> calculador.calcularSha256(diretorioTemporario))
                .isInstanceOf(FalhaCalculoHashException.class)
                .hasMessageContaining("é um diretório e não um arquivo");
    }

    @Test
    void deveLancarExcecaoQuandoEntradasForemNulas() {
        assertThatThrownBy(() -> calculador.calcularSha256((byte[]) null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> calculador.calcularSha256((Path) null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> calculador.calcularSha256((ByteArrayInputStream) null))
                .isInstanceOf(NullPointerException.class);
    }
}
