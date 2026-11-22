package br.com.tissguard.api;

import br.com.tissguard.api.dto.RespostaPadraoApi;
import br.com.tissguard.api.dto.ResultadoValidacaoXmlDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.config.PropriedadesTissGuard;
import br.com.tissguard.validacao.ResultadoValidacao;
import br.com.tissguard.validacao.ServicoValidacao;
import br.com.tissguard.validacao.ValidadorEntradaSegura;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ValidadorEntradaSegura validadorEntradaSegura;

    public ValidacaoController(ServicoValidacao servicoValidacao) {
        this(servicoValidacao, new ValidadorEntradaSegura(new PropriedadesTissGuard()));
    }

    @Autowired
    public ValidacaoController(ServicoValidacao servicoValidacao, ValidadorEntradaSegura validadorEntradaSegura) {
        this.servicoValidacao = Objects.requireNonNull(servicoValidacao, "O serviço de validação não pode ser nulo");
        this.validadorEntradaSegura = Objects.requireNonNull(validadorEntradaSegura, "O validador de entrada não pode ser nulo");
    }

    @PostMapping("/xml")
    public ResponseEntity<RespostaPadraoApi<ResultadoValidacaoXmlDto>> validarXml(
            @Valid @RequestBody SolicitacaoValidacaoXmlDto solicitacao
    ) {
        validadorEntradaSegura.validar(solicitacao);
        ResultadoValidacao resultado = servicoValidacao.validar(solicitacao);
        ResultadoValidacaoXmlDto dto = ResultadoValidacaoXmlDto.de(resultado);

        String mensagem = dto.valido()
                ? "Documento XML validado com sucesso contra o esquema XSD."
                : "Validação concluída com inconsistências estruturais identificadas.";

        return ResponseEntity.ok(RespostaPadraoApi.sucesso(dto, mensagem));
    }
}
