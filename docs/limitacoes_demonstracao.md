# Limitações Conhecidas da Demonstração

Este documento registra os limites técnicos e de escopo atuais do **TISS Guard** para manter transparência na demonstração técnica:

1. **Validação Estrutural vs. Regras de Negócio:** A validação XML/XSD comprova conformidade estrutural com o esquema selecionado, mas não substitui auditoria médica, regras contratuais entre operadora e prestador, tabelas TUSS vigentes ou cálculos financeiros de glosa.
2. **Exemplos Sintéticos:** A demonstração utiliza exclusivamente arquivos XML sintéticos e esquemas de referência locais. Dados reais de pacientes (PHI/PII) não devem ser submetidos à interface de demonstração.
3. **Agente Determinístico Local:** O `AgenteInvestigador` opera estritamente sobre ferramentas técnicas locais homologadas (`consultar_artefatos`, `validar_xml`, `comparar_esquemas`), sem envio de XML a serviços externos de IA e sem execução de código arbitrário.
4. **Catálogo em Memória:** O catálogo inicial é carregado em memória na inicialização da aplicação a partir dos artefatos locais verificados por hash SHA-256; não há banco de dados relacional externo configurado neste MVP.
5. **Comparação Estrutural:** O comparador analisa elementos, tipos e restrições suportadas entre dois XSDs compatíveis, indicando limitações quando uma mudança normativa exige análise semântica fora do XSD.
