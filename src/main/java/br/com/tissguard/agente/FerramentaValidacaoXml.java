package br.com.tissguard.agente;

import br.com.tissguard.api.dto.ResultadoValidacaoXmlDto;
import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.validacao.ResultadoValidacao;
import br.com.tissguard.validacao.ServicoValidacao;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FerramentaValidacaoXml implements FerramentaAgente<SolicitacaoValidacaoXmlDto, ResultadoValidacaoXmlDto> {

    private final ServicoValidacao servicoValidacao;

    public FerramentaValidacaoXml(ServicoValidacao servicoValidacao) {
        this.servicoValidacao = Objects.requireNonNull(servicoValidacao, "O serviço de validação não pode ser nulo");
    }

    @Override
    public String nome() {
        return "validar_xml";
    }

    @Override
    public String descricao() {
        return "Executa validação sintática e estrutural de um documento XML contra o esquema XSD correspondente da versão selecionada.";
    }

    @Override
    public ResultadoValidacaoXmlDto executar(SolicitacaoValidacaoXmlDto entrada) {
        Objects.requireNonNull(entrada, "A solicitação de validação não pode ser nula");
        ResultadoValidacao resultado = servicoValidacao.validar(entrada);
        return ResultadoValidacaoXmlDto.de(resultado);
    }
}
