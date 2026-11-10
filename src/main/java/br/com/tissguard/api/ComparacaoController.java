package br.com.tissguard.api;

import br.com.tissguard.api.dto.RespostaPadraoApi;
import br.com.tissguard.api.dto.ResultadoComparacaoEsquemasDto;
import br.com.tissguard.api.dto.SolicitacaoComparacaoEsquemasDto;
import br.com.tissguard.comparacao.ComparadorEsquemas;
import br.com.tissguard.comparacao.ResultadoComparacaoEsquema;
import br.com.tissguard.validacao.LocalizadorEsquemas;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/comparacao")
public class ComparacaoController {

    private final ComparadorEsquemas comparadorEsquemas;
    private final LocalizadorEsquemas localizadorEsquemas;

    public ComparacaoController(ComparadorEsquemas comparadorEsquemas, LocalizadorEsquemas localizadorEsquemas) {
        this.comparadorEsquemas = Objects.requireNonNull(comparadorEsquemas, "O comparador de esquemas não pode ser nulo");
        this.localizadorEsquemas = Objects.requireNonNull(localizadorEsquemas, "O localizador de esquemas não pode ser nulo");
    }

    @PostMapping("/esquemas")
    public ResponseEntity<RespostaPadraoApi<ResultadoComparacaoEsquemasDto>> compararEsquemas(
            @Valid @RequestBody SolicitacaoComparacaoEsquemasDto solicitacao
    ) {
        Path caminhoBase = localizadorEsquemas.localizarCaminho(
                solicitacao.versaoBase(),
                solicitacao.nomeArquivoBase()
        );

        Path caminhoComparado = localizadorEsquemas.localizarCaminho(
                solicitacao.versaoComparada(),
                solicitacao.nomeArquivoComparado()
        );

        ResultadoComparacaoEsquema resultado = comparadorEsquemas.comparar(
                caminhoBase,
                solicitacao.versaoBase(),
                caminhoComparado,
                solicitacao.versaoComparada()
        );

        ResultadoComparacaoEsquemasDto dto = ResultadoComparacaoEsquemasDto.de(resultado);

        String mensagem = dto.esquemasIdenticos()
                ? "Comparação concluída: os esquemas são estruturalmente idênticos."
                : (dto.compativelRetroativamente()
                ? "Comparação concluída: esquemas compatíveis com alterações aditivas."
                : "Alerta: comparação concluída com quebras de contrato detectadas.");

        return ResponseEntity.ok(RespostaPadraoApi.sucesso(dto, mensagem));
    }
}
