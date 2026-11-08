package br.com.tissguard.api.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record RespostaErroApi(
        Instant timestamp,
        int statusHttp,
        String erro,
        String mensagem,
        String caminho,
        List<String> detalhes
) {
    public RespostaErroApi {
        Objects.requireNonNull(timestamp, "O timestamp não pode ser nulo");
        Objects.requireNonNull(erro, "O nome do erro não pode ser nulo");
        Objects.requireNonNull(mensagem, "A mensagem de erro não pode ser nula");
        if (detalhes == null) {
            detalhes = Collections.emptyList();
        } else {
            detalhes = Collections.unmodifiableList(detalhes);
        }
    }

    public static RespostaErroApi erro(int statusHttp, String erro, String mensagem, String caminho, List<String> detalhes) {
        return new RespostaErroApi(Instant.now(), statusHttp, erro, mensagem, caminho, detalhes);
    }

    public static RespostaErroApi erroSimples(int statusHttp, String erro, String mensagem, String caminho) {
        return erro(statusHttp, erro, mensagem, caminho, Collections.emptyList());
    }
}
