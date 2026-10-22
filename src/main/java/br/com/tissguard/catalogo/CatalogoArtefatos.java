package br.com.tissguard.catalogo;

import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogoArtefatos {

    private final Map<String, MetadadosArtefato> repositorio = new ConcurrentHashMap<>();

    public void registrarArtefato(MetadadosArtefato artefato) {
        Objects.requireNonNull(artefato, "O artefato a ser registrado não pode ser nulo");

        if (repositorio.containsKey(artefato.id())) {
            throw new ArtefatoJaRegistradoException(
                    "Artefato com identificador '" + artefato.id() + "' já registrado no catálogo."
            );
        }

        boolean duplicadoPorVersaoEArquivo = repositorio.values().stream()
                .anyMatch(existente ->
                        existente.componente().equals(artefato.componente()) &&
                        existente.versaoDeclarada().equalsIgnoreCase(artefato.versaoDeclarada()) &&
                        existente.nomeArquivo().equalsIgnoreCase(artefato.nomeArquivo())
                );

        if (duplicadoPorVersaoEArquivo) {
            throw new ArtefatoJaRegistradoException(
                    "Já existe um artefato cadastrado para o componente " + artefato.componente() +
                    ", versão " + artefato.versaoDeclarada() + " e arquivo " + artefato.nomeArquivo() + "."
            );
        }

        repositorio.put(artefato.id(), artefato);
    }

    public List<MetadadosArtefato> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(repositorio.values()));
    }

    public Optional<MetadadosArtefato> buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(repositorio.get(id));
    }

    public List<MetadadosArtefato> buscarPorComponente(ComponenteTiss componente) {
        if (componente == null) {
            return Collections.emptyList();
        }
        return repositorio.values().stream()
                .filter(artefato -> artefato.componente().equals(componente))
                .toList();
    }

    public Optional<MetadadosArtefato> buscarPorVersaoENomeArquivo(String versao, String nomeArquivo) {
        if (versao == null || versao.isBlank() || nomeArquivo == null || nomeArquivo.isBlank()) {
            return Optional.empty();
        }
        return repositorio.values().stream()
                .filter(artefato ->
                        artefato.versaoDeclarada().equalsIgnoreCase(versao.trim()) &&
                        artefato.nomeArquivo().equalsIgnoreCase(nomeArquivo.trim()))
                .findFirst();
    }

    public int quantidadeArtefatos() {
        return repositorio.size();
    }
}
