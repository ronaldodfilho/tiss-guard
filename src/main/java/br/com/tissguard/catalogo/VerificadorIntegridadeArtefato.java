package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Component
public class VerificadorIntegridadeArtefato {

    private final CalculadorHash calculadorHash;

    public VerificadorIntegridadeArtefato(CalculadorHash calculadorHash) {
        this.calculadorHash = Objects.requireNonNull(calculadorHash, "O calculador de hash não pode ser nulo");
    }

    public ResultadoIntegridade verificar(MetadadosArtefato artefato, Path caminhoArquivo) {
        Objects.requireNonNull(artefato, "Os metadados do artefato não podem ser nulos");
        Objects.requireNonNull(caminhoArquivo, "O caminho do arquivo não pode ser nulo");

        if (!Files.exists(caminhoArquivo)) {
            return new ResultadoIntegridade(
                    artefato.id(),
                    EstadoIntegridade.CORROMPIDO,
                    artefato.hashSha256(),
                    null,
                    "Arquivo físico não encontrado no caminho informado: " + caminhoArquivo
            );
        }

        if (Files.isDirectory(caminhoArquivo)) {
            return new ResultadoIntegridade(
                    artefato.id(),
                    EstadoIntegridade.CORROMPIDO,
                    artefato.hashSha256(),
                    null,
                    "O caminho informado aponta para um diretório e não um arquivo: " + caminhoArquivo
            );
        }

        String hashCalculado = calculadorHash.calcularSha256(caminhoArquivo);

        if (hashCalculado.equalsIgnoreCase(artefato.hashSha256())) {
            return new ResultadoIntegridade(
                    artefato.id(),
                    EstadoIntegridade.INTEGRO,
                    artefato.hashSha256(),
                    hashCalculado,
                    "Arquivo íntegro. O hash SHA-256 coincide com o metadado registrado."
            );
        }

        return new ResultadoIntegridade(
                artefato.id(),
                EstadoIntegridade.CORROMPIDO,
                artefato.hashSha256(),
                hashCalculado,
                "Divergência de integridade detectada. Hash esperado: " + artefato.hashSha256() + ", hash obtido: " + hashCalculado
        );
    }
}
