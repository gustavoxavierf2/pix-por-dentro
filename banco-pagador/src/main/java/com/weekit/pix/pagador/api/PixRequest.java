package com.weekit.pix.pagador.api;

import java.math.BigDecimal;

/** Corpo do POST /pix: o que o app do João manda. */
public record PixRequest(String chave, BigDecimal valor, String mensagem) {
}
