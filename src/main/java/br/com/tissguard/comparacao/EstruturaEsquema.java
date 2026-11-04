package br.com.tissguard.comparacao;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record EstruturaEsquema(
        Map<String, DefinicaoElementoEsquema> elementos,
        Map<String, DefinicaoTipoEsquema> tipos
) {
    public EstruturaEsquema {
        Objects.requireNonNull(elementos, "O mapa de elementos não pode ser nulo");
        Objects.requireNonNull(tipos, "O mapa de tipos não pode ser nulo");
        elementos = Collections.unmodifiableMap(elementos);
        tipos = Collections.unmodifiableMap(tipos);
    }
}
