const XML_EXEMPLO_VALIDO = `<?xml version="1.0" encoding="UTF-8"?>
<ans:mensagemTISS xmlns:ans="http://www.ans.gov.br/padroes/tiss/schemas"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <ans:cabecalho>
        <ans:identificacaoTransacao>
            <ans:tipoTransacao>ENVIO_LOTE_GUIAS</ans:tipoTransacao>
            <ans:sequencialTransacao>123456</ans:sequencialTransacao>
            <ans:dataRegistroTransacao>2026-11-01</ans:dataRegistroTransacao>
            <ans:horaRegistroTransacao>14:30:00</ans:horaRegistroTransacao>
        </ans:identificacaoTransacao>
        <ans:origem>
            <ans:identificacaoPrestador>
                <ans:codigoPrestadorNaOperadora>PRESTADOR001</ans:codigoPrestadorNaOperadora>
            </ans:identificacaoPrestador>
        </ans:origem>
        <ans:destino>
            <ans:registroANS>123456</ans:registroANS>
        </ans:destino>
        <ans:versaoPadrao>04.03.00</ans:versaoPadrao>
    </ans:cabecalho>
</ans:mensagemTISS>`;

const XML_EXEMPLO_INVALIDO = `<?xml version="1.0" encoding="UTF-8"?>
<ans:mensagemTISS xmlns:ans="http://www.ans.gov.br/padroes/tiss/schemas"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <ans:cabecalho>
        <ans:identificacaoTransacao>
            <ans:tipoTransacao>TRANSACAO_INEXISTENTE</ans:tipoTransacao>
            <ans:sequencialTransacao>123456</ans:sequencialTransacao>
            <ans:dataRegistroTransacao>2026-11-01</ans:dataRegistroTransacao>
            <ans:horaRegistroTransacao>14:30:00</ans:horaRegistroTransacao>
        </ans:identificacaoTransacao>
        <ans:destino>
            <ans:registroANS>123456</ans:registroANS>
        </ans:destino>
        <ans:versaoPadrao>04.03.00</ans:versaoPadrao>
    </ans:cabecalho>
</ans:mensagemTISS>`;

document.addEventListener("DOMContentLoaded", () => {
    carregarCatalogo();
    carregarVersoes();
    configurarEventosValidacao();
    configurarEventosComparacao();
    configurarEventosAgente();
});

async function carregarCatalogo() {
    const corpoTabela = document.getElementById("corpo-tabela-artefatos");
    try {
        const resposta = await fetch("/api/v1/artefatos");
        const envelope = await resposta.json();
        const artefatos = envelope.dados || [];

        corpoTabela.innerHTML = "";
        if (artefatos.length === 0) {
            corpoTabela.innerHTML = `<tr><td colspan="6" class="texto-centro">Nenhum artefato registrado no catálogo.</td></tr>`;
            return;
        }

        artefatos.forEach(art => {
            const linha = document.createElement("tr");
            linha.innerHTML = `
                <td><strong>${escapeHtml(art.nome)}</strong></td>
                <td>${escapeHtml(art.componente)}</td>
                <td>${escapeHtml(art.versaoDeclarada)}</td>
                <td><code>${escapeHtml(art.nomeArquivo)}</code></td>
                <td><span class="badge ${art.estadoIntegridade === 'INTEGRO' ? 'badge-sucesso' : 'badge-alerta'}">${escapeHtml(art.estadoIntegridade)}</span></td>
                <td><small><code>${escapeHtml(art.hashSha256)}</code></small></td>
            `;
            corpoTabela.appendChild(linha);
        });
    } catch (e) {
        corpoTabela.innerHTML = `<tr><td colspan="6" class="texto-centro">Falha ao carregar catálogo oficial.</td></tr>`;
    }
}

async function carregarVersoes() {
    const selectValidacao = document.getElementById("select-versao-validacao");
    const selectBase = document.getElementById("select-versao-base");
    const selectComp = document.getElementById("select-versao-comparada");

    try {
        const resposta = await fetch("/api/v1/artefatos/versoes");
        const envelope = await resposta.json();
        const versoes = envelope.dados || [];

        [selectValidacao, selectBase, selectComp].forEach(sel => {
            sel.innerHTML = '<option value="">Selecione uma versão...</option>';
            versoes.forEach(v => {
                const opt = document.createElement("option");
                opt.value = v;
                opt.textContent = v;
                sel.appendChild(opt);
            });
        });

        if (versoes.includes("04.03.00")) {
            selectValidacao.value = "04.03.00";
            selectBase.value = "04.03.00";
        }
        if (versoes.includes("04.04.00")) {
            selectComp.value = "04.04.00";
        }
    } catch (e) {
        console.error("Erro ao carregar lista de versões", e);
    }
}

function configurarEventosValidacao() {
    const btnValido = document.getElementById("btn-exemplo-valido");
    const btnInvalido = document.getElementById("btn-exemplo-invalido");
    const txtXml = document.getElementById("textarea-xml");
    const formValidacao = document.getElementById("form-validacao");
    const btnAtualizarCat = document.getElementById("btn-atualizar-catalogo");

    btnValido.addEventListener("click", () => {
        txtXml.value = XML_EXEMPLO_VALIDO;
    });

    btnInvalido.addEventListener("click", () => {
        txtXml.value = XML_EXEMPLO_INVALIDO;
    });

    btnAtualizarCat.addEventListener("click", () => {
        carregarCatalogo();
        carregarVersoes();
    });

    formValidacao.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const versao = document.getElementById("select-versao-validacao").value;
        const nomeArquivo = document.getElementById("input-nome-arquivo").value;
        const conteudoXml = txtXml.value;

        const painel = document.getElementById("resultado-validacao");
        const badge = document.getElementById("badge-status-validacao");
        const titulo = document.getElementById("titulo-resultado-validacao");
        const resumo = document.getElementById("mensagem-resumo-validacao");
        const blocoErros = document.getElementById("bloco-erros-validacao");
        const corpoTabelaErros = document.getElementById("corpo-tabela-erros");

        painel.classList.remove("oculto");
        badge.className = "badge badge-aviso";
        badge.textContent = "Processando";
        titulo.textContent = "Validando contra esquema XSD...";
        resumo.textContent = "Aguardando resposta do validador estrutural...";
        blocoErros.classList.add("oculto");

        try {
            const resposta = await fetch("/api/v1/validacao/xml", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    nomeArquivo: nomeArquivo,
                    versaoEsquema: versao,
                    conteudoXml: conteudoXml
                })
            });

            const envelope = await resposta.json();
            const dados = envelope.dados;

            if (dados && dados.valido) {
                badge.className = "badge badge-sucesso";
                badge.textContent = "VÁLIDO";
                titulo.textContent = `Documento em conformidade com XSD ${dados.versaoEsquema}`;
                resumo.textContent = dados.mensagemInformativa || envelope.mensagem;
                blocoErros.classList.add("oculto");
            } else if (dados) {
                badge.className = "badge badge-alerta";
                badge.textContent = dados.status || "INVÁLIDO";
                titulo.textContent = `Inconsistências encontradas (${dados.totalErros} erro(s))`;
                resumo.textContent = dados.mensagemInformativa || envelope.mensagem;
                blocoErros.classList.remove("oculto");
                corpoTabelaErros.innerHTML = "";

                dados.erros.forEach((err, idx) => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td>${idx + 1}</td>
                        <td><strong>${err.linha || '-'}</strong></td>
                        <td>${err.coluna || '-'}</td>
                        <td><span class="badge badge-alerta">${escapeHtml(err.categoria || '')}</span></td>
                        <td>${escapeHtml(err.mensagem || '')}</td>
                    `;
                    corpoTabelaErros.appendChild(tr);
                });
            } else {
                badge.className = "badge badge-alerta";
                badge.textContent = "ERRO HTTP";
                titulo.textContent = envelope.erro || "Falha na validação";
                resumo.textContent = envelope.mensagem || "Erro inesperado na chamada da API.";
                blocoErros.classList.add("oculto");
            }
        } catch (e) {
            badge.className = "badge badge-alerta";
            badge.textContent = "FALHA";
            titulo.textContent = "Erro de conexão";
            resumo.textContent = "Não foi possível conectar ao endpoint de validação.";
            blocoErros.classList.add("oculto");
        }
    });
}

function configurarEventosComparacao() {
    const formComparacao = document.getElementById("form-comparacao");

    formComparacao.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const versaoBase = document.getElementById("select-versao-base").value;
        const versaoComp = document.getElementById("select-versao-comparada").value;

        const painel = document.getElementById("resultado-comparacao");
        const badge = document.getElementById("badge-status-comparacao");
        const titulo = document.getElementById("titulo-resultado-comparacao");
        const metricaTotal = document.getElementById("metrica-total-diferencas");
        const metricaQuebras = document.getElementById("metrica-quebras-contrato");
        const metricaAdicionados = document.getElementById("metrica-elementos-adicionados");
        const diagTexto = document.getElementById("diagnostico-comparacao-texto");
        const recTexto = document.getElementById("recomendacao-comparacao-texto");
        const corpoTabelaDiferencas = document.getElementById("corpo-tabela-diferencas");

        painel.classList.remove("oculto");
        badge.className = "badge badge-aviso";
        badge.textContent = "Comparando";
        titulo.textContent = `Comparando ${versaoBase} vs ${versaoComp}...`;

        try {
            const resposta = await fetch("/api/v1/comparacao/esquemas", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    versaoBase: versaoBase,
                    versaoComparada: versaoComp
                })
            });

            const envelope = await resposta.json();
            const dados = envelope.dados;

            if (dados) {
                const temQuebra = dados.totalQuebrasContrato > 0;
                badge.className = `badge ${temQuebra ? 'badge-alerta' : 'badge-sucesso'}`;
                badge.textContent = temQuebra ? "QUEBRA DE CONTRATO" : "COMPATÍVEL";
                titulo.textContent = `Diagnóstico: ${dados.versaoEsquemaBase} -> ${dados.versaoEsquemaComparado}`;

                metricaTotal.textContent = dados.totalDiferencas;
                metricaQuebras.textContent = dados.totalQuebrasContrato;
                metricaAdicionados.textContent = dados.totalAlteracoesAditivas;
                diagTexto.textContent = dados.resumoDiagnostico;
                recTexto.textContent = dados.recomendacaoIntegracao;

                corpoTabelaDiferencas.innerHTML = "";
                if (dados.diferencas && dados.diferencas.length > 0) {
                    dados.diferencas.forEach(dif => {
                        const tr = document.createElement("tr");
                        tr.innerHTML = `
                            <td><strong>${escapeHtml(dif.tipo || '')}</strong></td>
                            <td><code>${escapeHtml(dif.caminhoEstrutural || '')}</code></td>
                            <td><span class="badge ${dif.severidade === 'QUEBRA_DE_CONTRATO' ? 'badge-alerta' : 'badge-aviso'}">${escapeHtml(dif.severidade || '')}</span></td>
                            <td>${escapeHtml(dif.descricao || '')}</td>
                        `;
                        corpoTabelaDiferencas.appendChild(tr);
                    });
                } else {
                    corpoTabelaDiferencas.innerHTML = `<tr><td colspan="4" class="texto-centro">Nenhuma diferença estrutural encontrada entre os esquemas.</td></tr>`;
                }
            } else {
                badge.className = "badge badge-alerta";
                badge.textContent = "FALHA";
                titulo.textContent = envelope.erro || "Erro na comparação";
                diagTexto.textContent = envelope.mensagem || "Erro retornado pela API.";
            }
        } catch (e) {
            badge.className = "badge badge-alerta";
            badge.textContent = "ERRO";
            titulo.textContent = "Falha de comunicação";
            diagTexto.textContent = "Não foi possível conectar ao endpoint de comparação.";
        }
    });
}

function configurarEventosAgente() {
    const btnXml = document.getElementById("btn-agente-investigar-xml");
    const btnComp = document.getElementById("btn-agente-investigar-comparacao");
    const btnCat = document.getElementById("btn-agente-consultar-catalogo");
    const btnFora = document.getElementById("btn-agente-fora-escopo");

    btnXml.addEventListener("click", () => {
        const versao = document.getElementById("select-versao-validacao").value || "04.03.00";
        const nomeArquivo = document.getElementById("input-nome-arquivo").value || "mensagem.xml";
        const xml = document.getElementById("textarea-xml").value || XML_EXEMPLO_VALIDO;

        executarAgente({
            tipo: "VALIDACAO_XML",
            textoPergunta: "Investigue se este XML possui conformidade estrutural com o XSD e detalhe os erros se houver.",
            dadosValidacao: {
                nomeArquivo: nomeArquivo,
                versaoEsquema: versao,
                conteudoXml: xml
            }
        });
    });

    btnComp.addEventListener("click", () => {
        const base = document.getElementById("select-versao-base").value || "04.03.00";
        const comp = document.getElementById("select-versao-comparada").value || "04.04.00";

        executarAgente({
            tipo: "COMPARACAO_ESQUEMAS",
            textoPergunta: `Diagnostique o impacto e as quebras de contrato entre a versão ${base} e ${comp}.`,
            dadosComparacao: {
                versaoBase: base,
                versaoComparada: comp
            }
        });
    });

    btnCat.addEventListener("click", () => {
        executarAgente({
            tipo: "CONSULTA_ARTEFATO",
            textoPergunta: "Quais artefatos oficiais estão homologados no catálogo local?",
            dadosConsulta: {
                componente: "COMUNICACAO",
                versaoDeclarada: "04.03.00"
            }
        });
    });

    btnFora.addEventListener("click", () => {
        executarAgente({
            tipo: "FORA_DE_ESCOPO",
            textoPergunta: "Qual é o valor financeiro da glosa médica desta conta hospitalar?"
        });
    });
}

async function executarAgente(solicitacao) {
    const painel = document.getElementById("resultado-agente");
    const badge = document.getElementById("badge-status-agente");
    const titulo = document.getElementById("titulo-resultado-agente");
    const terminalFerramenta = document.getElementById("terminal-ferramenta");
    const terminalStatus = document.getElementById("terminal-evidencia-status");
    const terminalExplicacao = document.getElementById("terminal-explicacao");
    const terminalLimitacao = document.getElementById("terminal-limitacao");

    painel.classList.remove("oculto");
    badge.className = "badge badge-aviso";
    badge.textContent = "INVESTIGANDO";
    titulo.textContent = "Agente consultando ferramentas técnicas...";
    terminalFerramenta.textContent = "carregando...";
    terminalStatus.textContent = "analisando...";
    terminalExplicacao.textContent = "Processando solicitação...";
    terminalLimitacao.textContent = "";

    try {
        const resposta = await fetch("/api/v1/agente/investigar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(solicitacao)
        });

        const envelope = await resposta.json();
        const dados = envelope.dados;

        if (dados) {
            badge.className = `badge ${dados.evidenciaSuficiente ? 'badge-sucesso' : 'badge-alerta'}`;
            badge.textContent = dados.evidenciaSuficiente ? "EVIDÊNCIA COMPROVADA" : "SEM EVIDÊNCIA / RECUSA";
            titulo.textContent = dados.evidenciaSuficiente ? "Investigação Técnica Concluída" : "Limitação de Escopo ou Falta de Dados";

            terminalFerramenta.textContent = dados.ferramentaUtilizada;
            terminalStatus.textContent = dados.evidenciaSuficiente ? "Sim (100% determinística)" : "Não";
            terminalExplicacao.textContent = dados.explicacaoFundamentada;
            terminalLimitacao.textContent = dados.limitacaoOuAlerta || "Nenhuma limitação adicional.";
        } else {
            badge.className = "badge badge-alerta";
            badge.textContent = "ERRO";
            titulo.textContent = envelope.titulo || "Falha na chamada do agente";
            terminalExplicacao.textContent = envelope.detalhe || "Erro inesperado ao consultar agente.";
        }
    } catch (e) {
        badge.className = "badge badge-alerta";
        badge.textContent = "FALHA DE REDE";
        titulo.textContent = "Não foi possível conectar ao Agente Investigador";
        terminalExplicacao.textContent = "Erro de conexão com o endpoint /api/v1/agente/investigar.";
    }
}

function escapeHtml(texto) {
    if (!texto) return "";
    return String(texto)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
