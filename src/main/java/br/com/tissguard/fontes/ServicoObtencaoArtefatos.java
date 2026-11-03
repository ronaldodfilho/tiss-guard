package br.com.tissguard.fontes;

import br.com.tissguard.catalogo.CalculadorHash;
import br.com.tissguard.catalogo.CatalogoArtefatos;
import br.com.tissguard.catalogo.DetectorAlteracoesArtefato;
import br.com.tissguard.catalogo.ResultadoDeteccaoAlteracao;
import br.com.tissguard.catalogo.TipoAlteracaoArtefato;
import br.com.tissguard.dominio.EstadoIntegridade;
import br.com.tissguard.dominio.MetadadosArtefato;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class ServicoObtencaoArtefatos {

    private static final Path DIRETORIO_BASE_PADRAO = Path.of("dados", "artefatos");

    private final ClienteObtencaoArtefato clienteObtencao;
    private final CalculadorHash calculadorHash;
    private final DetectorAlteracoesArtefato detectorAlteracoes;
    private final CatalogoArtefatos catalogoArtefatos;

    public ServicoObtencaoArtefatos(
            ClienteObtencaoArtefato clienteObtencao,
            CalculadorHash calculadorHash,
            DetectorAlteracoesArtefato detectorAlteracoes,
            CatalogoArtefatos catalogoArtefatos
    ) {
        this.clienteObtencao = Objects.requireNonNull(clienteObtencao, "O cliente de obtenção não pode ser nulo");
        this.calculadorHash = Objects.requireNonNull(calculadorHash, "O calculador de hash não pode ser nulo");
        this.detectorAlteracoes = Objects.requireNonNull(detectorAlteracoes, "O detector de alterações não pode ser nulo");
        this.catalogoArtefatos = Objects.requireNonNull(catalogoArtefatos, "O catálogo de artefatos não pode ser nulo");
    }

    public ResultadoProcessamentoObtencao processar(SolicitacaoObtencaoArtefato solicitacao) {
        return processar(solicitacao, DIRETORIO_BASE_PADRAO);
    }

    public ResultadoProcessamentoObtencao processar(SolicitacaoObtencaoArtefato solicitacao, Path diretorioBase) {
        Objects.requireNonNull(solicitacao, "A solicitação de obtenção não pode ser nula");
        Objects.requireNonNull(diretorioBase, "O diretório base de armazenamento não pode ser nulo");

        ResultadoObtencaoArtefato resultadoHttp = clienteObtencao.obter(
                solicitacao.uriOrigem(),
                solicitacao.nomeArquivo()
        );

        String hashCalculado = calculadorHash.calcularSha256(resultadoHttp.conteudo());

        MetadadosArtefato metadados = new MetadadosArtefato(
                solicitacao.id(),
                solicitacao.nome(),
                solicitacao.componente(),
                solicitacao.versaoDeclarada(),
                solicitacao.uriOrigem().toString(),
                LocalDate.now(),
                solicitacao.dataPublicacao(),
                solicitacao.nomeArquivo(),
                hashCalculado,
                EstadoIntegridade.INTEGRO,
                solicitacao.observacoes()
        );

        ResultadoDeteccaoAlteracao deteccao = detectorAlteracoes.detectar(metadados);

        try {
            if (deteccao.tipo() == TipoAlteracaoArtefato.SEM_ALTERACAO) {
                Path caminhoExistente = diretorioBase
                        .resolve("versoes")
                        .resolve(solicitacao.versaoDeclarada())
                        .resolve(solicitacao.nomeArquivo());
                return new ResultadoProcessamentoObtencao(metadados, deteccao, caminhoExistente, false);
            }

            if (deteccao.tipo() == TipoAlteracaoArtefato.CONTEUDO_MODIFICADO) {
                Path caminhoAlerta = diretorioBase
                        .resolve("alertas_modificacao")
                        .resolve(solicitacao.versaoDeclarada() + "_" + solicitacao.nomeArquivo() + ".modificado");
                Files.createDirectories(caminhoAlerta.getParent());
                Files.write(caminhoAlerta, resultadoHttp.conteudo());
                return new ResultadoProcessamentoObtencao(metadados, deteccao, caminhoAlerta, true);
            }

            Path caminhoDestino = diretorioBase
                    .resolve("versoes")
                    .resolve(solicitacao.versaoDeclarada())
                    .resolve(solicitacao.nomeArquivo());
            Files.createDirectories(caminhoDestino.getParent());
            Files.write(caminhoDestino, resultadoHttp.conteudo());
            catalogoArtefatos.registrarArtefato(metadados);

            return new ResultadoProcessamentoObtencao(metadados, deteccao, caminhoDestino, true);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar artefato obtido no armazenamento local: " + e.getMessage(), e);
        }
    }
}
