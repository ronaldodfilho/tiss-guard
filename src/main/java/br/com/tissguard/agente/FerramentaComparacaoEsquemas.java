package br.com.tissguard.agente;

import br.com.tissguard.api.dto.ResultadoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.comparacao.ComparadorEsquemas;
import br.com.tissguard.comparacao.ResultadoComparacaoEsquema;
import br.com.tissguard.validacao.LocalizadorEsquemas;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

@Component
public class FerramentaComparacaoEsquemas implements FerramentaAgente<SolicitacaoComparacaoEsquemasDto, ResultadoComparacaoEsquemasDto> {

    private final ComparadorEsquemas comparadorEsquemas;
    private final LocalizadorEsquemas localizadorEsquemas;

    public FerramentaComparacaoEsquemas(ComparadorEsquemas comparadorEsquemas, LocalizadorEsquemas localizadorEsquemas) {
        this.comparadorEsquemas = Objects.requireNonNull(comparadorEsquemas, "O comparador de esquemas não pode ser nulo");
        this.localizadorEsquemas = Objects.requireNonNull(localizadorEsquemas, "O localizador de esquemas não pode ser nulo");
    }

    @Override
    public String nome() {
        return "comparar_esquemas";
    }

    @Override
    public String descricao() {
        return "Compara estruturalmente duas versões de esquemas XSD, fornecendo diferenças e diagnóstico de quebra de contrato.";
    }

    @Override
    public ResultadoComparacaoEsquemasDto executar(SolicitacaoComparacaoEsquemasDto entrada) {
        Objects.requireNonNull(entrada, "A solicitação de comparação não pode ser nula");

        Path caminhoBase = localizadorEsquemas.localizarCaminho(
                entrada.versaoBase(),
                entrada.nomeArquivoBase()
        );

        Path caminhoComparado = localizadorEsquemas.localizarCaminho(
                entrada.versaoComparada(),
                entrada.nomeArquivoComparado()
        );

        ResultadoComparacaoEsquema resultado = comparadorEsquemas.comparar(
                caminhoBase,
                entrada.versaoBase(),
                caminhoComparado,
                entrada.versaoComparada()
        );

        return ResultadoComparacaoEsquemasDto.de(resultado);
    }
}

