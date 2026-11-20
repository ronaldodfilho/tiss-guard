package br.com.tissguard.api;

import br.com.tissguard.agente.AgenteInvestigador;
import br.com.tissguard.agente.PerguntaInvestigacao;
import br.com.tissguard.agente.RespostaInvestigacao;
import br.com.tissguard.api.dto.RespostaPadraoApi;
import br.com.tissguard.api.dto.ResultadoInvestigacaoDto;
import br.com.tissguard.api.dto.SolicitacaoInvestigacaoDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/agente")
public class AgenteController {

    private final AgenteInvestigador agenteInvestigador;

    public AgenteController(AgenteInvestigador agenteInvestigador) {
        this.agenteInvestigador = Objects.requireNonNull(agenteInvestigador, "O agente investigador não pode ser nulo");
    }

    @PostMapping("/investigar")
    public ResponseEntity<RespostaPadraoApi<ResultadoInvestigacaoDto>> investigar(
            @Valid @RequestBody SolicitacaoInvestigacaoDto solicitacao
    ) {
        PerguntaInvestigacao pergunta = new PerguntaInvestigacao(
                solicitacao.tipo(),
                solicitacao.textoPergunta(),
                solicitacao.dadosValidacao(),
                solicitacao.dadosComparacao(),
                solicitacao.dadosConsulta()
        );

        RespostaInvestigacao resposta = agenteInvestigador.investigar(pergunta);
        ResultadoInvestigacaoDto dto = ResultadoInvestigacaoDto.de(resposta);

        return ResponseEntity.ok(RespostaPadraoApi.sucesso(dto, "Investigação concluída com base em evidências técnicas."));
    }
}
