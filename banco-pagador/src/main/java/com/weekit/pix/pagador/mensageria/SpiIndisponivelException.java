package com.weekit.pix.pagador.mensageria;

/** O broker (SPI) não aceitou a mensagem. Esconde a exceção do RabbitMQ do resto do código. */
public class SpiIndisponivelException extends RuntimeException {

    public SpiIndisponivelException(Throwable causa) {
        super("SPI indisponivel, tente novamente", causa);
    }
}
