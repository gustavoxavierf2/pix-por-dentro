package com.weekit.pix.recebedor.mensageria;

import com.weekit.pix.recebedor.mensageria.Mensagens.Pacs002;
import com.weekit.pix.recebedor.mensageria.Mensagens.Pacs008;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** PRODUCER: a única classe do recebedor que envia mensagens para o SPI. */
@Component
public class SpiPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpiPublisher.class);

    private final RabbitTemplate rabbit;

    public SpiPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    /** Responde ao banco que mandou a ordem: o endereço (routing key) vem do ISPB dele. */
    public void responder(Pacs008 ordem, Pacs002 resposta) {
        var routingKey = RabbitConfig.chavePacs002(ordem.pagador().ispb());
        rabbit.convertAndSend(RabbitConfig.SPI, routingKey, resposta);
        log.info("[SPI] -> pacs.002 {} enviada para o banco {}", resposta.status(), ordem.pagador().ispb());
    }
}
