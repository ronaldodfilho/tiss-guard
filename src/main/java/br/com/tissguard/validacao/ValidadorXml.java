package br.com.tissguard.validacao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ValidadorXml {

    private final LeitorXmlSeguro leitorXmlSeguro;

    public ValidadorXml() {
        this(new LeitorXmlSeguro());
    }

    @Autowired
    public ValidadorXml(LeitorXmlSeguro leitorXmlSeguro) {
        this.leitorXmlSeguro = Objects.requireNonNull(leitorXmlSeguro, "O leitor XML não pode ser nulo");
    }

    public ResultadoValidacao validar(DocumentoXml documentoXml, String versaoEsquema, Source fonteEsquema) {
        Objects.requireNonNull(documentoXml, "O documento XML não pode ser nulo");
        Objects.requireNonNull(versaoEsquema, "A versão do esquema não pode ser nula");
        Objects.requireNonNull(fonteEsquema, "A fonte do esquema XSD não pode ser nula");

        if (versaoEsquema.isBlank()) {
            throw new IllegalArgumentException("A versão do esquema não pode estar em branco");
        }

        Schema esquema = compilarEsquema(fonteEsquema);
        return executarValidacao(documentoXml, versaoEsquema, esquema);
    }

    public ResultadoValidacao validar(DocumentoXml documentoXml, String versaoEsquema, InputStream esquemaInputStream) {
        Objects.requireNonNull(esquemaInputStream, "O fluxo de entrada do esquema XSD não pode ser nulo");
        return validar(documentoXml, versaoEsquema, new StreamSource(esquemaInputStream));
    }

    public ResultadoValidacao validar(DocumentoXml documentoXml, String versaoEsquema, Path caminhoEsquema) {
        Objects.requireNonNull(caminhoEsquema, "O caminho do esquema XSD não pode ser nulo");
        if (!Files.exists(caminhoEsquema)) {
            throw new EsquemaNaoEncontradoException("Arquivo de esquema XSD não encontrado no caminho: " + caminhoEsquema);
        }
        try {
            return validar(documentoXml, versaoEsquema, new StreamSource(Files.newInputStream(caminhoEsquema)));
        } catch (IOException e) {
            throw new EsquemaInvalidoException("Falha ao ler o arquivo de esquema XSD: " + caminhoEsquema, e);
        }
    }

    public ResultadoValidacao validar(String nomeArquivo, byte[] conteudoBytes, String versaoEsquema, Source fonteEsquema) {
        DocumentoXml documentoXml = leitorXmlSeguro.ler(nomeArquivo, conteudoBytes);
        return validar(documentoXml, versaoEsquema, fonteEsquema);
    }

    public ResultadoValidacao validar(String nomeArquivo, String conteudoXml, String versaoEsquema, Source fonteEsquema) {
        DocumentoXml documentoXml = leitorXmlSeguro.ler(nomeArquivo, conteudoXml);
        return validar(documentoXml, versaoEsquema, fonteEsquema);
    }

    private Schema compilarEsquema(Source fonteEsquema) {
        SchemaFactory schemaFactory = criarFabricaEsquemaSegura();
        try {
            return schemaFactory.newSchema(fonteEsquema);
        } catch (SAXException e) {
            throw new EsquemaInvalidoException("Erro ao compilar esquema XSD: " + e.getMessage(), e);
        }
    }

    private ResultadoValidacao executarValidacao(DocumentoXml documentoXml, String versaoEsquema, Schema esquema) {
        List<ErroValidacao> erros = new ArrayList<>();

        try {
            Validator validador = esquema.newValidator();
            configurarValidadorSeguro(validador);

            validador.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                }

                @Override
                public void error(SAXParseException exception) {
                    erros.add(new ErroValidacao(
                            exception.getLineNumber(),
                            exception.getColumnNumber(),
                            normalizarMensagem(exception.getMessage())
                    ));
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    erros.add(new ErroValidacao(
                            exception.getLineNumber(),
                            exception.getColumnNumber(),
                            normalizarMensagem(exception.getMessage())
                    ));
                }
            });

            Source fonteDocumento = new StreamSource(new ByteArrayInputStream(documentoXml.conteudoBytes()));
            validador.validate(fonteDocumento);

        } catch (SAXException e) {
            if (erros.isEmpty()) {
                int linha = (e instanceof SAXParseException spe) ? spe.getLineNumber() : -1;
                int coluna = (e instanceof SAXParseException spe) ? spe.getColumnNumber() : -1;
                erros.add(new ErroValidacao(linha, coluna, normalizarMensagem(e.getMessage())));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha de E/S durante validação contra o esquema XSD", e);
        }

        StatusValidacao status = erros.isEmpty() ? StatusValidacao.VALIDO : StatusValidacao.INVALIDO;
        String mensagemInformativa = erros.isEmpty()
                ? "Documento XML válido de acordo com o esquema XSD versão " + versaoEsquema + "."
                : "Documento XML inválido: foram encontrados " + erros.size() + " erros de conformidade estrutural.";

        return new ResultadoValidacao(
                documentoXml.nomeArquivo(),
                versaoEsquema,
                status,
                erros,
                mensagemInformativa
        );
    }

    private SchemaFactory criarFabricaEsquemaSegura() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
        } catch (SAXException e) {
            throw new IllegalStateException("Configuração de segurança para SchemaFactory não suportada", e);
        }
        return factory;
    }

    private void configurarValidadorSeguro(Validator validador) {
        try {
            validador.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (SAXException e) {
            throw new IllegalStateException("Configuração de segurança para Validator não suportada", e);
        }
    }

    private String normalizarMensagem(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return "Erro estrutural não especificado no XML.";
        }
        return mensagem.trim();
    }
}
