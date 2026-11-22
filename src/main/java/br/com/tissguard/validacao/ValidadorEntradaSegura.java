package br.com.tissguard.validacao;

import br.com.tissguard.api.dto.SolicitacaoValidacaoXmlDto;
import br.com.tissguard.config.PropriedadesTissGuard;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class ValidadorEntradaSegura {

    private static final Pattern PADRAO_NOME_ARQUIVO = Pattern.compile("^[a-zA-Z0-9._-]+\\.xml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PADRAO_VERSAO = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final PropriedadesTissGuard propriedades;

    public ValidadorEntradaSegura(PropriedadesTissGuard propriedades) {
        this.propriedades = Objects.requireNonNull(propriedades, "As propriedades do sistema não podem ser nulas");
    }

    public void validar(SolicitacaoValidacaoXmlDto solicitacao) {
        Objects.requireNonNull(solicitacao, "A solicitação de validação não pode ser nula");

        validarNomeArquivo(solicitacao.nomeArquivo());
        validarVersao(solicitacao.versaoEsquema());
        validarTamanhoConteudo(solicitacao.conteudoXml());
    }

    public void validarNomeArquivo(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("O nome do arquivo XML é obrigatório.");
        }

        String limpo = nomeArquivo.trim();
        if (limpo.contains("..") || limpo.contains("/") || limpo.contains("\\") || limpo.contains("\0")) {
            throw new XmlSegurancaException("Nome de arquivo inválido: tentativa de travessia de diretório bloqueada.");
        }

        if (!PADRAO_NOME_ARQUIVO.matcher(limpo).matches()) {
            throw new XmlSegurancaException("Nome de arquivo não permitido. Utilize apenas caracteres seguros e extensão .xml.");
        }
    }

    public void validarVersao(String versao) {
        if (versao == null || versao.isBlank()) {
            throw new IllegalArgumentException("A versão do esquema é obrigatória.");
        }

        String limpa = versao.trim();
        if (limpa.contains("..") || !PADRAO_VERSAO.matcher(limpa).matches()) {
            throw new XmlSegurancaException("Identificador de versão contém caracteres não permitidos.");
        }
    }

    public void validarTamanhoConteudo(String conteudoXml) {
        if (conteudoXml == null || conteudoXml.isBlank()) {
            throw new IllegalArgumentException("O conteúdo XML não pode estar vazio.");
        }

        long tamanhoBytes = conteudoXml.getBytes(StandardCharsets.UTF_8).length;
        if (tamanhoBytes > propriedades.getTamanhoMaximoUploadBytes()) {
            throw new XmlSegurancaException(
                    "O tamanho do conteúdo XML (" + tamanhoBytes + " bytes) excede o limite máximo permitido de "
                            + propriedades.getTamanhoMaximoUploadBytes() + " bytes."
            );
        }
    }
}
