package br.com.tissguard.validacao;

public class XmlMalformadoException extends RuntimeException {

    private final int linha;
    private final int coluna;

    public XmlMalformadoException(String mensagem, int linha, int coluna, Throwable causa) {
        super(mensagem, causa);
        this.linha = linha;
        this.coluna = coluna;
    }

    public XmlMalformadoException(String mensagem, Throwable causa) {
        this(mensagem, -1, -1, causa);
    }

    public XmlMalformadoException(String mensagem) {
        this(mensagem, -1, -1, null);
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }
}
