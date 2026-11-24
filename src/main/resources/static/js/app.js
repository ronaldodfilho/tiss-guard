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
    configurarAbas();
    configurarValidacao();
    configurarComparacao();
    configurarAgente();
    carregarCatalogo();
    carregarVersoes();
});

function configurarAbas() {
    const botoes = document.querySelectorAll(".tab-btn");
    botoes.forEach(btn => {
        btn.addEventListener("click", () => {
            const tab = btn.dataset.tab;
            botoes.forEach(b => {
                const ativo = b.dataset.tab === tab;
                b.classList.toggle("active", ativo);
                b.setAttribute("aria-selected", String(ativo));
            });
            document.querySelectorAll(".panel").forEach(p => {
                p.hidden = p.id !== `painel-${tab}`;
            });
        });
    });

    document.getElementById("btn-atualizar-catalogo").addEventListener("click", () => {
        carregarCatalogo();
        carregarVersoes();
        exibirToast("Catálogo atualizado.");
    });
}

async function carregarCatalogo() {
    const corpo = document.getElementById("corpo-tabela-artefatos");
    try {
        const res = await fetch("/api/v1/artefatos");
        const envelope = await res.json();
        const artefatos = envelope.dados || [];

        corpo.innerHTML = "";
        if (artefatos.length === 0) {
            corpo.innerHTML = `<tr><td colspan="6" class="empty-cell">Nenhum artefato registrado.</td></tr>`;
            return;
        }

        artefatos.forEach(art => {
            const hashCurto = (art.hashSha256 || "").slice(0, 14) + "...";
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${escapeHtml(art.nome)}</strong></td>
                <td><span class="badge badge-aviso">${escapeHtml(art.versaoDeclarada)}</span></td>
                <td><code class="mono">${escapeHtml(art.nomeArquivo)}</code></td>
                <td><span class="badge ${art.estadoIntegridade === "INTEGRO" ? "badge-sucesso" : "badge-alerta"}">${escapeHtml(art.estadoIntegridade)}</span></td>
                <td><code class="mono" title="${escapeHtml(art.hashSha256)}">${escapeHtml(hashCurto)}</code></td>
                <td class="col-right"><button type="button" class="btn-copy" data-hash="${escapeHtml(art.hashSha256)}">Copiar</button></td>
            `;
            corpo.appendChild(tr);
        });

        corpo.querySelectorAll(".btn-copy").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    await navigator.clipboard.writeText(btn.dataset.hash);
                    exibirToast("Hash SHA-256 copiado.");
                } catch (e) {
                    exibirToast("Não foi possível copiar.");
                }
            });
        });
    } catch (e) {
        corpo.innerHTML = `<tr><td colspan="6" class="empty-cell">Erro ao carregar catálogo.</td></tr>`;
    }
}

async function carregarVersoes() {
    const selects = [
        document.getElementById("select-versao-validacao"),
        document.getElementById("select-versao-base"),
        document.getElementById("select-versao-comparada")
    ];

    try {
        const res = await fetch("/api/v1/artefatos/versoes");
        const envelope = await res.json();
        const versoes = envelope.dados || [];

        selects.forEach(sel => {
            sel.innerHTML = '<option value="">Selecione...</option>';
            versoes.forEach(v => {
                const opt = document.createElement("option");
                opt.value = v;
                opt.textContent = v;
                sel.appendChild(opt);
            });
        });

        if (versoes.includes("04.03.00")) {
            selects[0].value = "04.03.00";
            selects[1].value = "04.03.00";
        }
        if (versoes.includes("04.04.00")) {
            selects[2].value = "04.04.00";
        }
    } catch (e) {
        exibirToast("Falha ao carregar versões.");
    }
}

function configurarValidacao() {
    const txtXml = document.getElementById("textarea-xml");
    const inputNome = document.getElementById("input-nome-arquivo");
    const inputArquivo = document.getElementById("input-arquivo-xml");

    document.getElementById("btn-exemplo-valido").addEventListener("click", () => {
        txtXml.value = XML_EXEMPLO_VALIDO;
        inputNome.value = "guia_valida.xml";
    });

    document.getElementById("btn-exemplo-invalido").addEventListener("click", () => {
        txtXml.value = XML_EXEMPLO_INVALIDO;
        inputNome.value = "guia_invalida.xml";
    });

    document.getElementById("btn-limpar-xml").addEventListener("click", () => {
        txtXml.value = "";
    });

    inputArquivo.addEventListener("change", (ev) => {
        const arquivo = ev.target.files[0];
        if (!arquivo) return;
        const leitor = new FileReader();
        leitor.onload = (e) => {
            txtXml.value = e.target.result;
            inputNome.value = arquivo.name;
            exibirToast(`Arquivo "${arquivo.name}" carregado.`);
        };
        leitor.readAsText(arquivo, "UTF-8");
    });

    document.getElementById("form-validacao").addEventListener("submit", async (ev) => {
        ev.preventDefault();
        const painel = document.getElementById("resultado-validacao");
        const badge = document.getElementById("badge-status-validacao");
        const titulo = document.getElementById("titulo-resultado-validacao");
        const resumo = document.getElementById("mensagem-resumo-validacao");
        const blocoErros = document.getElementById("bloco-erros-validacao");
        const corpoErros = document.getElementById("corpo-tabela-erros");

        painel.classList.remove("oculto");
        badge.className = "badge badge-aviso";
        badge.textContent = "VALIDANDO";
        titulo.textContent = "Processando...";
        resumo.textContent = "";
        blocoErros.classList.add("oculto");

        try {
            const res = await fetch("/api/v1/validacao/xml", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    nomeArquivo: inputNome.value,
                    versaoEsquema: document.getElementById("select-versao-validacao").value,
                    conteudoXml: txtXml.value
                })
            });

            const envelope = await res.json();
            const dados = envelope.dados;

            if (dados && dados.valido) {
                badge.className = "badge badge-sucesso";
                badge.textContent = "VÁLIDO";
                titulo.textContent = `Conforme com o esquema ${dados.versaoEsquema}`;
                resumo.textContent = dados.mensagemInformativa || envelope.mensagem;
            } else if (dados) {
                badge.className = "badge badge-alerta";
                badge.textContent = dados.status || "INVÁLIDO";
                titulo.textContent = `${dados.totalErros} inconsistência(s) encontrada(s)`;
                resumo.textContent = dados.mensagemInformativa || envelope.mensagem;
                blocoErros.classList.remove("oculto");
                corpoErros.innerHTML = "";

                dados.erros.forEach((err, i) => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
                        <td class="mono">${i + 1}</td>
                        <td class="mono"><strong>${err.linha || "-"}</strong></td>
                        <td class="mono">${err.coluna || "-"}</td>
                        <td><span class="badge badge-alerta">${escapeHtml(err.categoria || "")}</span></td>
                        <td>${escapeHtml(err.mensagem || "")}</td>
                    `;
                    corpoErros.appendChild(tr);
                });
            } else {
                badge.className = "badge badge-alerta";
                badge.textContent = "ERRO";
                titulo.textContent = envelope.erro || "Falha na validação";
                resumo.textContent = envelope.mensagem || "";
            }
        } catch (e) {
            badge.className = "badge badge-alerta";
            badge.textContent = "ERRO";
            titulo.textContent = "Falha de conexão";
            resumo.textContent = "Não foi possível contatar a API.";
        }
    });
}

function configurarComparacao() {
    document.getElementById("form-comparacao").addEventListener("submit", async (ev) => {
        ev.preventDefault();
        const painel = document.getElementById("resultado-comparacao");
        const badge = document.getElementById("badge-status-comparacao");
        const titulo = document.getElementById("titulo-resultado-comparacao");
        const corpo = document.getElementById("corpo-tabela-diferencas");

        painel.classList.remove("oculto");
        badge.className = "badge badge-aviso";
        badge.textContent = "COMPARANDO";
        titulo.textContent = "Analisando esquemas...";

        try {
            const res = await fetch("/api/v1/comparacao/esquemas", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    versaoBase: document.getElementById("select-versao-base").value,
                    versaoComparada: document.getElementById("select-versao-comparada").value
                })
            });

            const envelope = await res.json();
            const dados = envelope.dados;

            if (dados) {
                const quebra = dados.totalQuebrasContrato > 0;
                badge.className = `badge ${quebra ? "badge-alerta" : "badge-sucesso"}`;
                badge.textContent = quebra ? "QUEBRA DE CONTRATO" : "COMPATÍVEL";
                titulo.textContent = `${dados.versaoEsquemaBase} -> ${dados.versaoEsquemaComparado}`;

                document.getElementById("metrica-total-diferencas").textContent = String(dados.totalDiferencas);
                document.getElementById("metrica-quebras-contrato").textContent = String(dados.totalQuebrasContrato);
                document.getElementById("metrica-elementos-adicionados").textContent = String(dados.totalAlteracoesAditivas);
                document.getElementById("diagnostico-comparacao-texto").textContent = dados.resumoDiagnostico;
                document.getElementById("recomendacao-comparacao-texto").textContent = dados.recomendacaoIntegracao;

                corpo.innerHTML = "";
                const lista = dados.diferencas || [];
                if (lista.length === 0) {
                    corpo.innerHTML = `<tr><td colspan="4" class="empty-cell">Nenhuma diferença encontrada.</td></tr>`;
                } else {
                    lista.forEach(dif => {
                        const cls = dif.severidade === "QUEBRA_DE_CONTRATO"
                            ? "badge-alerta"
                            : dif.severidade === "ADITIVA"
                                ? "badge-sucesso"
                                : "badge-aviso";
                        const tr = document.createElement("tr");
                        tr.innerHTML = `
                            <td class="mono">${escapeHtml(dif.tipo || "")}</td>
                            <td><code class="mono">${escapeHtml(dif.caminhoEstrutural || "")}</code></td>
                            <td><span class="badge ${cls}">${escapeHtml(dif.severidade || "")}</span></td>
                            <td>${escapeHtml(dif.descricao || "")}</td>
                        `;
                        corpo.appendChild(tr);
                    });
                }
            } else {
                badge.className = "badge badge-alerta";
                badge.textContent = "ERRO";
                titulo.textContent = envelope.erro || "Falha na comparação";
            }
        } catch (e) {
            badge.className = "badge badge-alerta";
            badge.textContent = "ERRO";
            titulo.textContent = "Erro ao conectar com o comparador.";
        }
    });
}

function configurarAgente() {
    document.getElementById("btn-agente-investigar-xml").addEventListener("click", () => {
        executarAgente({
            tipo: "VALIDACAO_XML",
            textoPergunta: "Investigue se este XML possui conformidade estrutural com o XSD.",
            dadosValidacao: {
                nomeArquivo: document.getElementById("input-nome-arquivo").value || "guia.xml",
                versaoEsquema: document.getElementById("select-versao-validacao").value || "04.03.00",
                conteudoXml: document.getElementById("textarea-xml").value || XML_EXEMPLO_VALIDO
            }
        });
    });

    document.getElementById("btn-agente-investigar-comparacao").addEventListener("click", () => {
        const base = document.getElementById("select-versao-base").value || "04.03.00";
        const comp = document.getElementById("select-versao-comparada").value || "04.04.00";
        executarAgente({
            tipo: "COMPARACAO_ESQUEMAS",
            textoPergunta: `Diagnostique quebras de contrato entre ${base} e ${comp}.`,
            dadosComparacao: { versaoBase: base, versaoComparada: comp }
        });
    });

    document.getElementById("btn-agente-consultar-catalogo").addEventListener("click", () => {
        executarAgente({
            tipo: "CONSULTA_ARTEFATO",
            textoPergunta: "Quais artefatos estão homologados para 04.03.00?",
            dadosConsulta: { componente: "COMUNICACAO", versaoDeclarada: "04.03.00" }
        });
    });

    document.getElementById("btn-agente-fora-escopo").addEventListener("click", () => {
        executarAgente({
            tipo: "FORA_DE_ESCOPO",
            textoPergunta: "Qual o valor financeiro glosado desta fatura hospitalar?"
        });
    });
}

async function executarAgente(solicitacao) {
    const painel = document.getElementById("resultado-agente");
    const badge = document.getElementById("badge-status-agente");
    const titulo = document.getElementById("titulo-resultado-agente");

    painel.classList.remove("oculto");
    badge.className = "badge badge-aviso";
    badge.textContent = "EXECUTANDO";
    titulo.textContent = "Consultando ferramenta...";

    try {
        const res = await fetch("/api/v1/agente/investigar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(solicitacao)
        });
        const envelope = await res.json();
        const dados = envelope.dados;

        if (dados) {
            badge.className = `badge ${dados.evidenciaSuficiente ? "badge-sucesso" : "badge-alerta"}`;
            badge.textContent = dados.evidenciaSuficiente ? "EVIDÊNCIA COMPROVADA" : "SEM EVIDÊNCIA";
            titulo.textContent = dados.evidenciaSuficiente ? "Investigação Concluída" : "Fora de Escopo ou Dados Insuficientes";
            document.getElementById("terminal-ferramenta").textContent = dados.ferramentaUtilizada;
            document.getElementById("terminal-evidencia-status").textContent = dados.evidenciaSuficiente ? "Sim (determinística)" : "Não";
            document.getElementById("terminal-explicacao").textContent = dados.explicacaoFundamentada;
            document.getElementById("terminal-limitacao").textContent = dados.limitacaoOuAlerta || "-";
            document.getElementById("terminal-json-bruto").textContent = JSON.stringify(dados, null, 2);
        } else {
            badge.className = "badge badge-alerta";
            badge.textContent = "ERRO";
            titulo.textContent = envelope.erro || "Erro na consulta";
        }
    } catch (e) {
        badge.className = "badge badge-alerta";
        badge.textContent = "ERRO";
        titulo.textContent = "Falha de conexão com o agente.";
    }
}

function exibirToast(msg) {
    const c = document.getElementById("toast-container");
    const el = document.createElement("div");
    el.className = "toast";
    el.textContent = msg;
    c.appendChild(el);
    setTimeout(() => el.remove(), 2600);
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
