package com.weekit.pix.pagador.api;

import com.weekit.pix.pagador.mensageria.SpiIndisponivelException;
import com.weekit.pix.pagador.pix.PixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz os erros das regras de negócio para status HTTP. É o único lugar que conhece os dois mundos. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PixException.class)
    ProblemDetail pixBarrado(PixException e) {
        log.warn("[ERRO] {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(status(e.motivo()), e.getMessage());
    }

    @ExceptionHandler(SpiIndisponivelException.class)
    ProblemDetail spiForaDoAr(SpiIndisponivelException e) {
        log.error("[ERRO] {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    private static HttpStatus status(PixException.Motivo motivo) {
        return switch (motivo) {
            case CHAVE_NAO_ENCONTRADA, TRANSACAO_NAO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case VALOR_INVALIDO -> HttpStatus.BAD_REQUEST;
            case LIMITE_EXCEDIDO, SALDO_INSUFICIENTE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
