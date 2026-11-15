package br.com.tissguard.agente;

import br.com.tissguard.api.dto.ArtefatoCatalogoDto;
import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FerramentaConsultaArtefatos implements FerramentaAgente<FerramentaConsultaArtefatos.Parametros, List<ArtefatoCatalogoDto>> {

    public record Parametros(ComponenteTiss componente, String versao) {}

    private final CatalogoArtefatos catalogo;

    public FerramentaConsultaArtefatos(CatalogoArtefatos catalogo) {
        this.catalogo = Objects.requireNonNull(catalogo, "O catálogo não pode ser nulo");
    }

    @Override
    public String nome() {
        return "consultar_artefatos";
    }

    @Override
    public String descricao() {
        return "Consulta fontes, versões declaradas e hashes oficiais dos artefatos TISS registrados no catálogo.";
    }

    @Override
    public List<ArtefatoCatalogoDto> executar(Parametros entrada) {
        List<MetadadosArtefato> lista;

        if (entrada != null && entrada.componente() != null) {
            lista = catalogo.buscarPorComponente(entrada.componente());
        } else if (entrada != null && entrada.versao() != null && !entrada.versao().isBlank()) {
            lista = catalogo.buscarPorVersao(entrada.versao());
        } else {
            lista = catalogo.listarTodos();
        }

        return lista.stream()
                .map(ArtefatoCatalogoDto::de)
                .toList();
    }
}
