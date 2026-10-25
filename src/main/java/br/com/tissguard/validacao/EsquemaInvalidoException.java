package br.com.tissguard.validacao;

public class EsquemaInvalidoException extends RuntimeException {

    public EsquemaInvalidoException(String mensagem) {
        super(mensagem);
    }

    public EsquemaInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
