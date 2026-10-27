package br.com.tissguard.validacao;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TradutorMensagemEsquema {

    private static final Pattern PADRAO_ELEMENTO_INCOMPLETO = Pattern.compile(
            "cvc-complex-type\\.2\\.4\\.b.*?element '([^']+)' is not complete.*?One of '([^']+)' is expected",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PADRAO_CONTEUDO_INVALIDO = Pattern.compile(
            "cvc-complex-type\\.2\\.4\\.a.*?starting with element '([^']+)'.*?One of '([^']+)' is expected",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PADRAO_PADRAO_FACET = Pattern.compile(
            "cvc-pattern-valid.*?Value '([^']*)' is not facet-valid with respect to pattern '([^']+)'",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PADRAO_TIPO_DADO = Pattern.compile(
            "cvc-datatype-valid\\.1\\.2\\.1.*?'([^']*)' is not a valid value for '([^']+)'",
            Pattern.CASE_INSENSITIVE
    );

    public String traduzir(String mensagemOriginal) {
        if (mensagemOriginal == null || mensagemOriginal.isBlank()) {
            return "Inconformidade estrutural não especificada no documento XML.";
        }

        String mensagemLimpa = mensagemOriginal.trim();

        Matcher matcherIncompleto = PADRAO_ELEMENTO_INCOMPLETO.matcher(mensagemLimpa);
        if (matcherIncompleto.find()) {
            String elementoPai = limparNomeElemento(matcherIncompleto.group(1));
            String esperado = limparNomeElemento(matcherIncompleto.group(2));
            return "Elemento obrigatório ausente ou incompleto: no elemento '" + elementoPai + "', esperava-se: " + esperado + ".";
        }

        Matcher matcherInvalido = PADRAO_CONTEUDO_INVALIDO.matcher(mensagemLimpa);
        if (matcherInvalido.find()) {
            String elementoEncontrado = limparNomeElemento(matcherInvalido.group(1));
            String esperado = limparNomeElemento(matcherInvalido.group(2));
            return "Elemento inesperado: foi encontrado '" + elementoEncontrado + "', mas esperava-se: " + esperado + ".";
        }

        Matcher matcherPadrao = PADRAO_PADRAO_FACET.matcher(mensagemLimpa);
        if (matcherPadrao.find()) {
            String valor = matcherPadrao.group(1);
            String formato = matcherPadrao.group(2);
            return "Valor inválido: o valor '" + valor + "' não atende ao padrão esperado '" + formato + "'.";
        }

        Matcher matcherTipo = PADRAO_TIPO_DADO.matcher(mensagemLimpa);
        if (matcherTipo.find()) {
            String valor = matcherTipo.group(1);
            String tipo = matcherTipo.group(2);
            return "Tipo de dado incompatível: o valor '" + valor + "' não é válido para o tipo '" + tipo + "'.";
        }

        return mensagemLimpa;
    }

    private String limparNomeElemento(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replaceAll("\"http://[^\"]+\":", "");
    }
}
