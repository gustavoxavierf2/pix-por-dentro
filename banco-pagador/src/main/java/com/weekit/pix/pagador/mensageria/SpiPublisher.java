package com.weekit.pix.pagador.mensageria;

import com.weekit.pix.pagador.mensageria.Mensagens.Pacs008;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** PRODUCER: a única classe do pagador que envia mensagens para o SPI. */
@Component
public class SpiPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpiPublisher.class);

    private final RabbitTemplate rabbit;

    public SpiPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    /** Publica a ordem de pagamento e volta na hora: NÃO espera o banco recebedor. */
    public void enviarOrdemDePagamento(Pacs008 pacs008) {
        var routingKey = RabbitConfig.chavePacs008(pacs008.recebedor().ispb());
        try {
            rabbit.convertAndSend(RabbitConfig.SPI, routingKey, pacs008);
        } catch (AmqpException e) {
            throw new SpiIndisponivelException(e);
        }
        log.info("[SPI] -> pacs.008 enviada: {} (routing key {})", pacs008.endToEndId(), routingKey);
    }
}
