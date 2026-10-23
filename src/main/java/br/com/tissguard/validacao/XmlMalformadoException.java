package br.com.tissguard.validacao;

public class XmlMalformadoException extends RuntimeException {

    public XmlMalformadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public XmlMalformadoException(String mensagem) {
        super(mensagem);
    }
}
