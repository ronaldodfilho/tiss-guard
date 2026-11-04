package br.com.tissguard.comparacao;

import br.com.tissguard.catalogo.CalculadorHash;
import br.com.tissguard.validacao.DocumentoXml;
import br.com.tissguard.validacao.LeitorXmlSeguro;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ComparadorEsquemas {

    private final LeitorXmlSeguro leitorXmlSeguro;
    private final CalculadorHash calculadorHash;

    public ComparadorEsquemas() {
        this(new LeitorXmlSeguro(), new CalculadorHash());
    }

    public ComparadorEsquemas(LeitorXmlSeguro leitorXmlSeguro, CalculadorHash calculadorHash) {
        this.leitorXmlSeguro = Objects.requireNonNull(leitorXmlSeguro, "O leitor XML seguro não pode ser nulo");
        this.calculadorHash = Objects.requireNonNull(calculadorHash, "O calculador de hash não pode ser nulo");
    }

    public ResultadoComparacaoEsquema comparar(Path caminhoBase, String versaoBase, Path caminhoComparado, String versaoComparado) {
        Objects.requireNonNull(caminhoBase, "O caminho do esquema base não pode ser nulo");
        Objects.requireNonNull(versaoBase, "A versão do esquema base não pode ser nula");
        Objects.requireNonNull(caminhoComparado, "O caminho do esquema comparado não pode ser nulo");
        Objects.requireNonNull(versaoComparado, "A versão do esquema comparado não pode ser nula");

        if (!Files.exists(caminhoBase) || Files.isDirectory(caminhoBase)) {
            throw new IllegalArgumentException("O caminho do esquema base não existe ou é um diretório: " + caminhoBase);
        }
        if (!Files.exists(caminhoComparado) || Files.isDirectory(caminhoComparado)) {
            throw new IllegalArgumentException("O caminho do esquema comparado não existe ou é um diretório: " + caminhoComparado);
        }

        try {
            byte[] bytesBase = Files.readAllBytes(caminhoBase);
            byte[] bytesComparado = Files.readAllBytes(caminhoComparado);

            String hashBase = calculadorHash.calcularSha256(bytesBase);
            String hashComparado = calculadorHash.calcularSha256(bytesComparado);

            String idBase = caminhoBase.getFileName().toString();
            String idComparado = caminhoComparado.getFileName().toString();

            return comparar(idBase, versaoBase, hashBase, bytesBase, idComparado, versaoComparado, hashComparado, bytesComparado);
        } catch (IOException e) {
            throw new IllegalStateException("Falha de I/O ao ler esquemas para comparação: " + e.getMessage(), e);
        }
    }

    public ResultadoComparacaoEsquema comparar(
            String idBase,
            String versaoBase,
            String hashBase,
            byte[] conteudoBase,
            String idComparado,
            String versaoComparado,
            String hashComparado,
            byte[] conteudoComparado
    ) {
        Objects.requireNonNull(idBase, "O identificador do esquema base não pode ser nulo");
        Objects.requireNonNull(versaoBase, "A versão do esquema base não pode ser nula");
        Objects.requireNonNull(hashBase, "O hash do esquema base não pode ser nulo");
        Objects.requireNonNull(conteudoBase, "O conteúdo do esquema base não pode ser nulo");
        Objects.requireNonNull(idComparado, "O identificador do esquema comparado não pode ser nulo");
        Objects.requireNonNull(versaoComparado, "A versão do esquema comparado não pode ser nula");
        Objects.requireNonNull(hashComparado, "O hash do esquema comparado não pode ser nulo");
        Objects.requireNonNull(conteudoComparado, "O conteúdo do esquema comparado não pode ser nulo");

        DocumentoXml docBase = leitorXmlSeguro.ler(idBase, conteudoBase);
        DocumentoXml docComparado = leitorXmlSeguro.ler(idComparado, conteudoComparado);

        validarSeEhEsquema(docBase.documento(), idBase);
        validarSeEhEsquema(docComparado.documento(), idComparado);

        EstruturaEsquema estruturaBase = extrairEstrutura(docBase.documento());
        EstruturaEsquema estruturaComparada = extrairEstrutura(docComparado.documento());

        List<DiferencaEsquema> diferencas = new ArrayList<>();

        compararElementos(estruturaBase.elementos(), estruturaComparada.elementos(), diferencas);
        compararTipos(estruturaBase.tipos(), estruturaComparada.tipos(), diferencas);

        diferencas.sort(Comparator.comparing(DiferencaEsquema::caminhoEstrutural).thenComparing(d -> d.tipo().name()));

        boolean identicos = diferencas.isEmpty() && hashBase.equalsIgnoreCase(hashComparado);

        return new ResultadoComparacaoEsquema(
                idBase,
                versaoBase,
                hashBase,
                idComparado,
                versaoComparado,
                hashComparado,
                diferencas,
                identicos,
                diferencas.size()
        );
    }

    private void validarSeEhEsquema(Document doc, String nomeArquivo) {
        Element raiz = doc.getDocumentElement();
        String localName = extrairNomeLocal(raiz);
        if (!"schema".equalsIgnoreCase(localName)) {
            throw new IllegalArgumentException(
                    "O arquivo '" + nomeArquivo + "' não é um esquema XML válido. Elemento raiz encontrado: " + raiz.getNodeName()
            );
        }
    }

    private EstruturaEsquema extrairEstrutura(Document doc) {
        Map<String, DefinicaoElementoEsquema> elementos = new LinkedHashMap<>();
        Map<String, DefinicaoTipoEsquema> tipos = new LinkedHashMap<>();

        Element raiz = doc.getDocumentElement();
        NodeList filhos = raiz.getChildNodes();

        for (int i = 0; i < filhos.getLength(); i++) {
            Node no = filhos.item(i);
            if (no.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element elementoFilho = (Element) no;
            String nomeLocal = extrairNomeLocal(elementoFilho);

            if ("element".equalsIgnoreCase(nomeLocal)) {
                processarElementoGlobal(elementoFilho, elementos);
            } else if ("complexType".equalsIgnoreCase(nomeLocal)) {
                processarTipoComplexoGlobal(elementoFilho, tipos, elementos);
            } else if ("simpleType".equalsIgnoreCase(nomeLocal)) {
                processarTipoSimplesGlobal(elementoFilho, tipos);
            }
        }

        return new EstruturaEsquema(elementos, tipos);
    }

    private void processarElementoGlobal(Element elem, Map<String, DefinicaoElementoEsquema> elementos) {
        String nome = elem.getAttribute("name");
        if (nome.isBlank()) {
            return;
        }

        String tipo = elem.getAttribute("type");
        String minOccurs = elem.hasAttribute("minOccurs") ? elem.getAttribute("minOccurs") : "1";
        String maxOccurs = elem.hasAttribute("maxOccurs") ? elem.getAttribute("maxOccurs") : "1";

        elementos.put(nome, new DefinicaoElementoEsquema(nome, nome, tipo, minOccurs, maxOccurs));

        processarEstruturaFilhaElemento(elem, nome, elementos);
    }

    private void processarTipoComplexoGlobal(
            Element elem,
            Map<String, DefinicaoTipoEsquema> tipos,
            Map<String, DefinicaoElementoEsquema> elementos
    ) {
        String nomeTipo = elem.getAttribute("name");
        if (nomeTipo.isBlank()) {
            return;
        }

        tipos.put(nomeTipo, new DefinicaoTipoEsquema(nomeTipo, "COMPLEXO", ""));
        processarElementosDentroDeConteiner(elem, nomeTipo, elementos);
    }

    private void processarTipoSimplesGlobal(Element elem, Map<String, DefinicaoTipoEsquema> tipos) {
        String nomeTipo = elem.getAttribute("name");
        if (nomeTipo.isBlank()) {
            return;
        }

        String tipoBase = "";
        NodeList filhos = elem.getChildNodes();
        for (int i = 0; i < filhos.getLength(); i++) {
            Node no = filhos.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE && "restriction".equalsIgnoreCase(extrairNomeLocal((Element) no))) {
                tipoBase = ((Element) no).getAttribute("base");
                break;
            }
        }

        tipos.put(nomeTipo, new DefinicaoTipoEsquema(nomeTipo, "SIMPLES", tipoBase));
    }

    private void processarEstruturaFilhaElemento(Element elem, String prefixoCaminho, Map<String, DefinicaoElementoEsquema> elementos) {
        NodeList filhos = elem.getChildNodes();
        for (int i = 0; i < filhos.getLength(); i++) {
            Node no = filhos.item(i);
            if (no.getNodeType() == Node.ELEMENT_NODE && "complexType".equalsIgnoreCase(extrairNomeLocal((Element) no))) {
                processarElementosDentroDeConteiner((Element) no, prefixoCaminho, elementos);
            }
        }
    }

    private void processarElementosDentroDeConteiner(Element conteiner, String prefixoCaminho, Map<String, DefinicaoElementoEsquema> elementos) {
        NodeList filhos = conteiner.getChildNodes();
        for (int i = 0; i < filhos.getLength(); i++) {
            Node no = filhos.item(i);
            if (no.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element filhoElem = (Element) no;
            String nomeLocal = extrairNomeLocal(filhoElem);

            if ("element".equalsIgnoreCase(nomeLocal)) {
                String nome = filhoElem.getAttribute("name");
                if (!nome.isBlank()) {
                    String tipo = filhoElem.getAttribute("type");
                    String minOccurs = filhoElem.hasAttribute("minOccurs") ? filhoElem.getAttribute("minOccurs") : "1";
                    String maxOccurs = filhoElem.hasAttribute("maxOccurs") ? filhoElem.getAttribute("maxOccurs") : "1";
                    String caminho = prefixoCaminho + "/" + nome;

                    elementos.put(caminho, new DefinicaoElementoEsquema(caminho, nome, tipo, minOccurs, maxOccurs));
                    processarEstruturaFilhaElemento(filhoElem, caminho, elementos);
                }
            } else if ("sequence".equalsIgnoreCase(nomeLocal) ||
                       "choice".equalsIgnoreCase(nomeLocal) ||
                       "all".equalsIgnoreCase(nomeLocal) ||
                       "complexContent".equalsIgnoreCase(nomeLocal) ||
                       "extension".equalsIgnoreCase(nomeLocal)) {
                processarElementosDentroDeConteiner(filhoElem, prefixoCaminho, elementos);
            }
        }
    }

    private String extrairNomeLocal(Element elem) {
        String local = elem.getLocalName();
        if (local != null && !local.isBlank()) {
            return local;
        }
        String tagName = elem.getTagName();
        int idx = tagName.indexOf(':');
        return idx >= 0 ? tagName.substring(idx + 1) : tagName;
    }

    private void compararElementos(
            Map<String, DefinicaoElementoEsquema> base,
            Map<String, DefinicaoElementoEsquema> comparado,
            List<DiferencaEsquema> diferencas
    ) {
        for (Map.Entry<String, DefinicaoElementoEsquema> entry : base.entrySet()) {
            String caminho = entry.getKey();
            DefinicaoElementoEsquema elemBase = entry.getValue();

            if (!comparado.containsKey(caminho)) {
                diferencas.add(new DiferencaEsquema(
                        TipoDiferencaEsquema.ELEMENTO_REMOVIDO,
                        caminho,
                        "Elemento '" + elemBase.nome() + "' removido do esquema",
                        elemBase.tipo(),
                        null
                ));
            } else {
                DefinicaoElementoEsquema elemComparado = comparado.get(caminho);

                if (!elemBase.tipo().equalsIgnoreCase(elemComparado.tipo())) {
                    diferencas.add(new DiferencaEsquema(
                            TipoDiferencaEsquema.ELEMENTO_MODIFICADO,
                            caminho,
                            "Tipo do elemento '" + elemBase.nome() + "' alterado de '" + elemBase.tipo() + "' para '" + elemComparado.tipo() + "'",
                            elemBase.tipo(),
                            elemComparado.tipo()
                    ));
                }

                if (!elemBase.ocorrenciaMinima().equalsIgnoreCase(elemComparado.ocorrenciaMinima())) {
                    diferencas.add(new DiferencaEsquema(
                            TipoDiferencaEsquema.ELEMENTO_MODIFICADO,
                            caminho,
                            "Ocorrência mínima (minOccurs) do elemento '" + elemBase.nome() + "' alterada de '" + elemBase.ocorrenciaMinima() + "' para '" + elemComparado.ocorrenciaMinima() + "'",
                            elemBase.ocorrenciaMinima(),
                            elemComparado.ocorrenciaMinima()
                    ));
                }

                if (!elemBase.ocorrenciaMaxima().equalsIgnoreCase(elemComparado.ocorrenciaMaxima())) {
                    diferencas.add(new DiferencaEsquema(
                            TipoDiferencaEsquema.ELEMENTO_MODIFICADO,
                            caminho,
                            "Ocorrência máxima (maxOccurs) do elemento '" + elemBase.nome() + "' alterada de '" + elemBase.ocorrenciaMaxima() + "' para '" + elemComparado.ocorrenciaMaxima() + "'",
                            elemBase.ocorrenciaMaxima(),
                            elemComparado.ocorrenciaMaxima()
                    ));
                }
            }
        }

        for (Map.Entry<String, DefinicaoElementoEsquema> entry : comparado.entrySet()) {
            String caminho = entry.getKey();
            DefinicaoElementoEsquema elemComparado = entry.getValue();

            if (!base.containsKey(caminho)) {
                diferencas.add(new DiferencaEsquema(
                        TipoDiferencaEsquema.ELEMENTO_ADICIONADO,
                        caminho,
                        "Elemento '" + elemComparado.nome() + "' adicionado ao esquema",
                        null,
                        elemComparado.tipo()
                ));
            }
        }
    }

    private void compararTipos(
            Map<String, DefinicaoTipoEsquema> base,
            Map<String, DefinicaoTipoEsquema> comparado,
            List<DiferencaEsquema> diferencas
    ) {
        for (Map.Entry<String, DefinicaoTipoEsquema> entry : base.entrySet()) {
            String nomeTipo = entry.getKey();
            DefinicaoTipoEsquema tipoBase = entry.getValue();

            if (!comparado.containsKey(nomeTipo)) {
                diferencas.add(new DiferencaEsquema(
                        TipoDiferencaEsquema.TIPO_REMOVIDO,
                        nomeTipo,
                        "Tipo '" + nomeTipo + "' removido do esquema",
                        tipoBase.categoria(),
                        null
                ));
            } else {
                DefinicaoTipoEsquema tipoComparado = comparado.get(nomeTipo);
                if (!tipoBase.categoria().equalsIgnoreCase(tipoComparado.categoria())) {
                    diferencas.add(new DiferencaEsquema(
                            TipoDiferencaEsquema.TIPO_MODIFICADO,
                            nomeTipo,
                            "Categoria do tipo '" + nomeTipo + "' alterada de '" + tipoBase.categoria() + "' para '" + tipoComparado.categoria() + "'",
                            tipoBase.categoria(),
                            tipoComparado.categoria()
                    ));
                } else if (!tipoBase.tipoBase().equalsIgnoreCase(tipoComparado.tipoBase())) {
                    diferencas.add(new DiferencaEsquema(
                            TipoDiferencaEsquema.TIPO_MODIFICADO,
                            nomeTipo,
                            "Tipo base da restrição de '" + nomeTipo + "' alterado de '" + tipoBase.tipoBase() + "' para '" + tipoComparado.tipoBase() + "'",
                            tipoBase.tipoBase(),
                            tipoComparado.tipoBase()
                    ));
                }
            }
        }

        for (Map.Entry<String, DefinicaoTipoEsquema> entry : comparado.entrySet()) {
            String nomeTipo = entry.getKey();
            DefinicaoTipoEsquema tipoComparado = entry.getValue();

            if (!base.containsKey(nomeTipo)) {
                diferencas.add(new DiferencaEsquema(
                        TipoDiferencaEsquema.TIPO_ADICIONADO,
                        nomeTipo,
                        "Tipo '" + nomeTipo + "' adicionado ao esquema",
                        null,
                        tipoComparado.categoria()
                ));
            }
        }
    }
}
