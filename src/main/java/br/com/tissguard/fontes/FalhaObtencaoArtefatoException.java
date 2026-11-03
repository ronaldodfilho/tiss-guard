package br.com.tissguard.fontes;

public class FalhaObtencaoArtefatoException extends RuntimeException {

    public FalhaObtencaoArtefatoException(String mensagem) {
        super(mensagem);
    }

    public FalhaObtencaoArtefatoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
