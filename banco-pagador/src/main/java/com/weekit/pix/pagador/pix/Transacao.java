package com.weekit.pix.pagador.pix;

import com.weekit.pix.pagador.mensageria.Mensagens.Pacs008;

import java.math.BigDecimal;

/** Um Pix do ponto de vista do app do João. `mensagemEnviada` é a pacs.008 que foi para o SPI. */
public record Transacao(String endToEndId, String para, String banco, BigDecimal valor,
                        Status status, String motivo, Pacs008 mensagemEnviada) {

    public enum Status { EM_PROCESSAMENTO, CONCLUIDO, REJEITADO }

    Transacao comStatus(Status novo, String motivo) {
        return new Transacao(endToEndId, para, banco, valor, novo, motivo, mensagemEnviada);
    }
}
