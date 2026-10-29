package br.com.tissguard.catalogo;

public class FalhaCalculoHashException extends RuntimeException {

    public FalhaCalculoHashException(String mensagem) {
        super(mensagem);
    }

    public FalhaCalculoHashException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
