package br.com.tissguard.dominio;

public enum ComponenteTiss {
    ORGANIZACIONAL("Organizacional"),
    CONTEUDO_E_ESTRUTURA("Conteúdo e Estrutura"),
    REPRESENTACAO_CONCEITOS_TUSS("Representação de Conceitos em Saúde (TUSS)"),
    SEGURANCA_E_PRIVACIDADE("Segurança e Privacidade"),
    COMUNICACAO("Comunicação");

    private final String descricao;

    ComponenteTiss(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
