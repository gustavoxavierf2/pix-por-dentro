package com.weekit.pix.recebedor.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O que fazer quando o consumidor falha: tenta 3 vezes (ver application.properties)
 * e, se continuar falhando, manda a mensagem para a DLQ em vez de travar a fila.
 */
@Configuration
public class DlqConfig {

    private static final Logger log = LoggerFactory.getLogger(DlqConfig.class);

    /** Chamado quando as 3 tentativas falham: rejeita sem reenfileirar, e o RabbitMQ manda para a DLQ. */
    @Bean
    MessageRecoverer enviarParaDlq() {
        return (message, causa) -> {
            log.error("[DLQ] 3 tentativas falharam. Mensagem enviada para a fila {} para analise manual.",
                    RabbitConfig.FILA_PACS008_RECEBEDOR_DLQ);
            throw new AmqpRejectAndDontRequeueException("Enviando para a DLQ", causa);
        };
    }
}
