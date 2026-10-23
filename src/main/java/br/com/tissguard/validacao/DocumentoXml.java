package br.com.tissguard.validacao;

import org.w3c.dom.Document;

import java.util.Objects;

public record DocumentoXml(
        String nomeArquivo,
        byte[] conteudoBytes,
        Document documento,
        String elementoRaiz,
        long tamanhoBytes
) {
    public DocumentoXml {
        Objects.requireNonNull(nomeArquivo, "O nome do arquivo não pode ser nulo");
        Objects.requireNonNull(conteudoBytes, "O conteúdo em bytes não pode ser nulo");
        Objects.requireNonNull(documento, "O documento DOM não pode ser nulo");
        Objects.requireNonNull(elementoRaiz, "O elemento raiz não pode ser nulo");

        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo não pode estar em branco");
        }
        if (elementoRaiz.isBlank()) {
            throw new IllegalArgumentException("O elemento raiz não pode estar em branco");
        }
        if (tamanhoBytes <= 0) {
            throw new IllegalArgumentException("O tamanho em bytes deve ser maior que zero");
        }
    }
}
