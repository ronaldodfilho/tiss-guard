package br.com.tissguard.validacao;

public class XmlSegurancaException extends RuntimeException {

    public XmlSegurancaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public XmlSegurancaException(String mensagem) {
        super(mensagem);
    }
}
