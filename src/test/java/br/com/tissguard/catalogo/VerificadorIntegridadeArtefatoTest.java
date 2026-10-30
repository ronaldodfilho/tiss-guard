package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificadorIntegridadeArtefatoTest {

    private VerificadorIntegridadeArtefato verificador;
    private CalculadorHash calculadorHash;

    @BeforeEach
    void configurar() {
        calculadorHash = new CalculadorHash();
        verificador = new VerificadorIntegridadeArtefato(calculadorHash);
    }

    private MetadadosArtefato criarArtefato(String hash) {
        return new MetadadosArtefato(
                "art-teste",
                "Esquema Teste",
                ComponenteTiss.COMUNICACAO,
                "04.03.00",
                "https://gov.br/ans",
                LocalDate.now(),
                LocalDate.of(2024, 4, 1),
                "teste.xsd",
                hash,
                EstadoIntegridade.INTEGRO,
                "Observacao"
        );
    }

    @Test
    void deveConfirmarIntegridadeQuandoHashForIdentico(@TempDir Path pastaTemporaria) throws IOException {
        Path arquivo = pastaTemporaria.resolve("esquema.xsd");
        Files.writeString(arquivo, "<schema>conteudo valido</schema>");
        String hashReal = calculadorHash.calcularSha256(arquivo);

        MetadadosArtefato artefato = criarArtefato(hashReal);

        ResultadoIntegridade resultado = verificador.verificar(artefato, arquivo);

        assertThat(resultado.isIntegro()).isTrue();
        assertThat(resultado.estado()).isEqualTo(EstadoIntegridade.INTEGRO);
        assertThat(resultado.hashCalculado()).isEqualTo(hashReal);
        assertThat(resultado.hashEsperado()).isEqualTo(hashReal);
    }

    @Test
    void deveDetectarDivergenciaQuandoHashForDiferente(@TempDir Path pastaTemporaria) throws IOException {
        Path arquivo = pastaTemporaria.resolve("esquema_alterado.xsd");
        Files.writeString(arquivo, "<schema>conteudo alterado</schema>");
        String hashFalso = "0000000000000000000000000000000000000000000000000000000000000000";

        MetadadosArtefato artefato = criarArtefato(hashFalso);

        ResultadoIntegridade resultado = verificador.verificar(artefato, arquivo);

        assertThat(resultado.isIntegro()).isFalse();
        assertThat(resultado.estado()).isEqualTo(EstadoIntegridade.CORROMPIDO);
        assertThat(resultado.hashCalculado()).isNotEqualTo(hashFalso);
        assertThat(resultado.mensagem()).contains("Divergência de integridade detectada");
    }

    @Test
    void deveRetornarCorrompidoQuandoArquivoNaoExistir(@TempDir Path pastaTemporaria) {
        Path arquivoInexistente = pastaTemporaria.resolve("nao_existe.xsd");
        MetadadosArtefato artefato = criarArtefato("hashqualquer");

        ResultadoIntegridade resultado = verificador.verificar(artefato, arquivoInexistente);

        assertThat(resultado.isIntegro()).isFalse();
        assertThat(resultado.estado()).isEqualTo(EstadoIntegridade.CORROMPIDO);
        assertThat(resultado.hashCalculado()).isNull();
        assertThat(resultado.mensagem()).contains("Arquivo físico não encontrado");
    }

    @Test
    void deveRetornarCorrompidoQuandoCaminhoForDiretorio(@TempDir Path pastaTemporaria) {
        MetadadosArtefato artefato = criarArtefato("hashqualquer");

        ResultadoIntegridade resultado = verificador.verificar(artefato, pastaTemporaria);

        assertThat(resultado.isIntegro()).isFalse();
        assertThat(resultado.estado()).isEqualTo(EstadoIntegridade.CORROMPIDO);
        assertThat(resultado.mensagem()).contains("aponta para um diretório");
    }

    @Test
    void deveLancarExcecaoQuandoArgumentosForemNulos() {
        MetadadosArtefato artefato = criarArtefato("hash");

        assertThatThrownBy(() -> verificador.verificar(null, Path.of("caminho")))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> verificador.verificar(artefato, null))
                .isInstanceOf(NullPointerException.class);
    }
}
