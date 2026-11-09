package br.com.tissguard.api;

import br.com.tissguard.api.dto.ArtefatoCatalogoDto;
import br.com.tissguard.api.dto.RespostaPadraoApi;
import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/artefatos")
public class ArtefatosController {

    private final CatalogoArtefatos catalogoArtefatos;

    public ArtefatosController(CatalogoArtefatos catalogoArtefatos) {
        this.catalogoArtefatos = Objects.requireNonNull(catalogoArtefatos, "O catálogo de artefatos não pode ser nulo");
    }

    @GetMapping
    public ResponseEntity<RespostaPadraoApi<List<ArtefatoCatalogoDto>>> listarTodos(
            @RequestParam(required = false) ComponenteTiss componente
    ) {
        List<MetadadosArtefato> artefatos = (componente != null)
                ? catalogoArtefatos.buscarPorComponente(componente)
                : catalogoArtefatos.listarTodos();

        List<ArtefatoCatalogoDto> dtos = artefatos.stream()
                .map(ArtefatoCatalogoDto::de)
                .toList();

        return ResponseEntity.ok(RespostaPadraoApi.sucesso(dtos, "Artefatos recuperados com sucesso."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RespostaPadraoApi<ArtefatoCatalogoDto>> buscarPorId(@PathVariable String id) {
        MetadadosArtefato artefato = catalogoArtefatos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Artefato não encontrado no catálogo para o identificador: " + id));

        return ResponseEntity.ok(RespostaPadraoApi.sucesso(ArtefatoCatalogoDto.de(artefato), "Artefato localizado."));
    }

    @GetMapping("/versoes")
    public ResponseEntity<RespostaPadraoApi<List<String>>> listarVersoes(
            @RequestParam(defaultValue = "COMUNICACAO") ComponenteTiss componente
    ) {
        List<String> versoes = catalogoArtefatos.listarVersoesPorComponente(componente);
        return ResponseEntity.ok(RespostaPadraoApi.sucesso(versoes, "Versões recuperadas com sucesso para o componente " + componente + "."));
    }
}
