package com.weekit.pix.pagador.mensageria;

import com.weekit.pix.pagador.mensageria.Mensagens.Pacs002;
import com.weekit.pix.pagador.pix.PixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** CONSUMER: a única classe do pagador que recebe mensagens do SPI. Só repassa para a regra de negócio. */
@Component
public class SpiListener {

    private static final Logger log = LoggerFactory.getLogger(SpiListener.class);

    private final PixService pixService;

    public SpiListener(PixService pixService) {
        this.pixService = pixService;
    }

    @RabbitListener(queues = RabbitConfig.FILA_PACS002_PAGADOR)
    public void receberResposta(Pacs002 resposta) {
        log.info("[SPI] <- pacs.002 recebida: {} status={}", resposta.endToEndId(), resposta.status());
        pixService.processarResposta(resposta);
    }
}
