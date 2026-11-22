package br.com.tissguard.validacao;

import org.springframework.stereotype.Component;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Component
public class LocalizadorEsquemas {

    public Source localizarFonte(String versao, String nomeArquivo) {
        Objects.requireNonNull(versao, "A versão do esquema não pode ser nula");
        validarSegmentoSeguro(versao);

        String arquivoResolvido = resolverArquivoSeguro(versao, nomeArquivo);

        Optional<Path> caminhoDisco = buscarEmDisco(versao, arquivoResolvido);
        if (caminhoDisco.isPresent()) {
            try {
                return new StreamSource(Files.newInputStream(caminhoDisco.get()), caminhoDisco.get().toUri().toString());
            } catch (Exception e) {
                throw new EsquemaInvalidoException("Falha ao abrir esquema localizado em disco: " + caminhoDisco.get(), e);
            }
        }

        InputStream streamClasspath = buscarNoClasspath(arquivoResolvido);
        if (streamClasspath != null) {
            return new StreamSource(streamClasspath, "classpath:/esquemas_xsd/" + arquivoResolvido);
        }

        throw new EsquemaNaoEncontradoException(
                "Esquema XSD não encontrado para a versão '" + versao + "' e arquivo '" + arquivoResolvido + "'."
        );
    }

    public Path localizarCaminho(String versao, String nomeArquivo) {
        Objects.requireNonNull(versao, "A versão do esquema não pode ser nula");
        validarSegmentoSeguro(versao);

        String arquivoResolvido = resolverArquivoSeguro(versao, nomeArquivo);

        Optional<Path> caminhoDisco = buscarEmDisco(versao, arquivoResolvido);
        if (caminhoDisco.isPresent()) {
            return caminhoDisco.get();
        }

        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            String alternativo = arquivoResolvido.replace(".xsd", "_evolucao.xsd");
            Optional<Path> caminhoAlternativo = buscarEmDisco(versao, alternativo);
            if (caminhoAlternativo.isPresent()) {
                return caminhoAlternativo.get();
            }
        }

        throw new EsquemaNaoEncontradoException(
                "Arquivo físico do esquema XSD não encontrado para a versão '" + versao + "' e arquivo '" + arquivoResolvido + "'."
        );
    }

    private String resolverArquivoSeguro(String versao, String nomeArquivo) {
        if (nomeArquivo != null && !nomeArquivo.isBlank()) {
            String limpo = nomeArquivo.trim();
            validarSegmentoSeguro(limpo);
            return limpo;
        }
        return resolverNomePadrao(versao);
    }

    private void validarSegmentoSeguro(String segmento) {
        if (segmento.contains("..") || segmento.contains("/") || segmento.contains("\\") || segmento.contains("\0")) {
            throw new XmlSegurancaException("Parâmetro de localização contém caracteres de travessia de diretório não permitidos.");
        }
    }

    private Optional<Path> buscarEmDisco(String versao, String nomeArquivo) {
        Path caminhoVersoes = Path.of("dados", "artefatos", "versoes", versao, nomeArquivo);
        if (Files.exists(caminhoVersoes)) {
            return Optional.of(caminhoVersoes);
        }

        Path caminhoMainResources = Path.of("src", "main", "resources", "esquemas_xsd", nomeArquivo);
        if (Files.exists(caminhoMainResources)) {
            return Optional.of(caminhoMainResources);
        }

        Path caminhoTestResources = Path.of("src", "test", "resources", "esquemas_xsd", nomeArquivo);
        if (Files.exists(caminhoTestResources)) {
            return Optional.of(caminhoTestResources);
        }

        return Optional.empty();
    }

    private InputStream buscarNoClasspath(String nomeArquivo) {
        InputStream is = getClass().getResourceAsStream("/esquemas_xsd/" + nomeArquivo);
        if (is != null) {
            return is;
        }
        return getClass().getResourceAsStream("/" + nomeArquivo);
    }

    public String resolverNomePadrao(String versao) {
        Objects.requireNonNull(versao, "A versão não pode ser nula");
        String formatada = versao.trim().replace(".", "_");
        if (formatada.startsWith("0")) {
            formatada = formatada.substring(1);
        }
        return "tissV" + formatada + ".xsd";
    }
}
