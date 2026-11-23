package br.com.tissguard.config;

import br.com.tissguard.catalogo.CalculadorHash;
import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.dominio.ComponenteTiss;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class InicializadorCatalogoDemonstracao {

    private static final String ORIGEM_OFICIAL_ANS = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss";

    private final CatalogoArtefatos catalogoArtefatos;
    private final CalculadorHash calculadorHash;

    public InicializadorCatalogoDemonstracao(CatalogoArtefatos catalogoArtefatos, CalculadorHash calculadorHash) {
        this.catalogoArtefatos = Objects.requireNonNull(catalogoArtefatos, "O catálogo de artefatos não pode ser nulo");
        this.calculadorHash = Objects.requireNonNull(calculadorHash, "O calculador de hash não pode ser nulo");
    }

    @PostConstruct
    public void inicializarCatalogoPadrao() {
        if (!catalogoArtefatos.listarTodos().isEmpty()) {
            return;
        }

        registrarSePresente(
                "art-tiss-04-03-00",
                "Esquema de Comunicação TISS 04.03.00",
                "04.03.00",
                LocalDate.of(2024, 4, 1),
                "tissV4_03_00.xsd",
                "Artefato de referência local da versão 04.03.00 para validação estrutural."
        );

        registrarSePresente(
                "art-tiss-04-04-00",
                "Esquema de Comunicação TISS 04.04.00 (Evolução)",
                "04.04.00",
                LocalDate.of(2024, 10, 1),
                "tissV4_04_00_evolucao.xsd",
                "Artefato de evolução estrutural utilizado para análise de compatibilidade e quebra de contrato."
        );
    }

    private void registrarSePresente(
            String id,
            String nome,
            String versao,
            LocalDate dataPublicacao,
            String nomeArquivo,
            String observacoes
    ) {
        try (InputStream stream = getClass().getResourceAsStream("/esquemas_xsd/" + nomeArquivo)) {
            if (stream == null) {
                return;
            }
            String hash = calculadorHash.calcularSha256(stream);
            MetadadosArtefato metadados = new MetadadosArtefato(
                    id,
                    nome,
                    ComponenteTiss.COMUNICACAO,
                    versao,
                    ORIGEM_OFICIAL_ANS,
                    LocalDate.now(),
                    dataPublicacao,
                    nomeArquivo,
                    hash,
                    EstadoIntegridade.INTEGRO,
                    observacoes
            );
            catalogoArtefatos.registrarArtefato(metadados);
        } catch (Exception ignored) {
        }
    }
}
