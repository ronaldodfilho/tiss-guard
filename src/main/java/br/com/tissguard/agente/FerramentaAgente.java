package br.com.tissguard.agente;

public interface FerramentaAgente<E, S> {
    String nome();
    String descricao();
    S executar(E entrada);
}
