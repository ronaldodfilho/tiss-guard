package br.com.tissguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tissguard")
public class PropriedadesTissGuard {

    private long tamanhoMaximoUploadBytes = 5_242_880L;
    private String diretorioArtefatos = "dados/artefatos";
    private int timeoutValidacaoSegundos = 10;

    public long getTamanhoMaximoUploadBytes() {
        return tamanhoMaximoUploadBytes;
    }

    public void setTamanhoMaximoUploadBytes(long tamanhoMaximoUploadBytes) {
        this.tamanhoMaximoUploadBytes = tamanhoMaximoUploadBytes;
    }

    public String getDiretorioArtefatos() {
        return diretorioArtefatos;
    }

    public void setDiretorioArtefatos(String diretorioArtefatos) {
        this.diretorioArtefatos = diretorioArtefatos;
    }

    public int getTimeoutValidacaoSegundos() {
        return timeoutValidacaoSegundos;
    }

    public void setTimeoutValidacaoSegundos(int timeoutValidacaoSegundos) {
        this.timeoutValidacaoSegundos = timeoutValidacaoSegundos;
    }
}
