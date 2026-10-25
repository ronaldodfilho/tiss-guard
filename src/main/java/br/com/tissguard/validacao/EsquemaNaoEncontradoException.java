package br.com.tissguard.validacao;

public class EsquemaNaoEncontradoException extends RuntimeException {

    public EsquemaNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public EsquemaNaoEncontradoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
