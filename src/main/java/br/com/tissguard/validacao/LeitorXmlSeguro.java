package br.com.tissguard.validacao;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class LeitorXmlSeguro {

    private static final long LIMITE_PADRAO_BYTES = 10L * 1024L * 1024L;

    private final long limiteMaximoBytes;

    public LeitorXmlSeguro() {
        this(LIMITE_PADRAO_BYTES);
    }

    public LeitorXmlSeguro(long limiteMaximoBytes) {
        if (limiteMaximoBytes <= 0) {
            throw new IllegalArgumentException("O limite de tamanho deve ser maior que zero");
        }
        this.limiteMaximoBytes = limiteMaximoBytes;
    }

    public DocumentoXml ler(String nomeArquivo, String conteudoXml) {
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");
        Objects.requireNonNull(conteudoXml, "O conteúdo XML não pode ser nulo");

        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo não pode estar em branco");
        }
        if (conteudoXml.isBlank()) {
            throw new IllegalArgumentException("O conteúdo XML não pode estar em branco");
        }

        byte[] bytes = conteudoXml.getBytes(StandardCharsets.UTF_8);
        return ler(nomeArquivo, bytes);
    }

    public DocumentoXml ler(String nomeArquivo, byte[] conteudoBytes) {
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");
        Objects.requireNonNull(conteudoBytes, "O conteúdo em bytes não pode ser nulo");

        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo não pode estar em branco");
        }
        if (conteudoBytes.length == 0) {
            throw new IllegalArgumentException("O conteúdo em bytes não pode estar vazio");
        }

        if (conteudoBytes.length > limiteMaximoBytes) {
            throw new XmlLimiteExcedidoException(
                    "O arquivo '" + nomeArquivo + "' com tamanho de " + conteudoBytes.length +
                    " bytes excede o limite máximo permitido de " + limiteMaximoBytes + " bytes."
            );
        }

        DocumentBuilderFactory factory = criarFabricaSegura();

        try (InputStream inputStream = new ByteArrayInputStream(conteudoBytes)) {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document documento = builder.parse(inputStream);
            documento.getDocumentElement().normalize();

            String elementoRaiz = documento.getDocumentElement().getNodeName();

            return new DocumentoXml(
                    nomeArquivo,
                    conteudoBytes,
                    documento,
                    elementoRaiz,
                    conteudoBytes.length
            );
        } catch (SAXException e) {
            String mensagem = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (mensagem.contains("doctype") || mensagem.contains("entity") || mensagem.contains("entidade")) {
                throw new XmlSegurancaException("Tentativa de declaração DOCTYPE ou entidade não permitida no arquivo: " + nomeArquivo, e);
            }
            throw new XmlMalformadoException("Arquivo XML malformado: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Falha técnica ao configurar parser XML", e);
        } catch (IOException e) {
            throw new XmlMalformadoException("Falha de leitura no conteúdo XML: " + e.getMessage(), e);
        }
    }

    private DocumentBuilderFactory criarFabricaSegura() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Configuração de segurança XML não suportada pelo ambiente", e);
        }

        return factory;
    }

    public long getLimiteMaximoBytes() {
        return limiteMaximoBytes;
    }
}
