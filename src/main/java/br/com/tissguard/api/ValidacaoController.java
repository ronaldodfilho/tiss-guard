package br.com.tissguard.api;

import br.com.tissguard.api.dto.RespostaPadraoApi;
import br.com.tissguard.api.dto.ResultadoValidacaoXmlDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.validacao.ResultadoValidacao;
import br.com.tissguard.validacao.ServicoValidacao;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/validacao")
public class ValidacaoController {

    private final ServicoValidacao servicoValidacao;

    public ValidacaoController(ServicoValidacao servicoValidacao) {
        this.servicoValidacao = Objects.requireNonNull(servicoValidacao, "O serviço de validação não pode ser nulo");
    }

    @PostMapping("/xml")
    public ResponseEntity<RespostaPadraoApi<ResultadoValidacaoXmlDto>> validarXml(
            @Valid @RequestBody SolicitacaoValidacaoXmlDto solicitacao
    ) {
        ResultadoValidacao resultado = servicoValidacao.validar(solicitacao);
        ResultadoValidacaoXmlDto dto = ResultadoValidacaoXmlDto.de(resultado);

        String mensagem = dto.valido()
                ? "Documento XML validado com sucesso contra o esquema XSD."
                : "Validação concluída com inconsistências estruturais identificadas.";

        return ResponseEntity.ok(RespostaPadraoApi.sucesso(dto, mensagem));
    }
}
