package com.weekit.pix.recebedor.mensageria;

import com.weekit.pix.recebedor.mensageria.Mensagens.Pacs008;
import com.weekit.pix.recebedor.pix.RecebimentoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** CONSUMER: a única classe do recebedor que recebe mensagens do SPI. Só repassa para a regra de negócio. */
@Component
public class SpiListener {

    private static final Logger log = LoggerFactory.getLogger(SpiListener.class);

    private final RecebimentoService recebimento;
    private final long atrasoMs;

    public SpiListener(RecebimentoService recebimento, @Value("${pix.atraso-processamento-ms}") long atrasoMs) {
        this.recebimento = recebimento;
        this.atrasoMs = atrasoMs;
    }

    /**
     * Se este método lançar exceção, o RabbitMQ tenta de novo (3x) e depois manda para a DLQ.
     * Se terminar normalmente, a mensagem é confirmada (ack) e some da fila.
     */
    @RabbitListener(queues = RabbitConfig.FILA_PACS008_RECEBEDOR)
    public void receberOrdem(Pacs008 ordem) throws InterruptedException {
        log.info("[SPI] <- pacs.008 recebida: {} | R$ {} de {} para conta {}",
                ordem.endToEndId(), ordem.valor(), ordem.pagador().nome(), ordem.recebedor().conta());

        // Só para dar tempo de ver a mensagem na fila do painel do RabbitMQ
        Thread.sleep(atrasoMs);

        recebimento.receber(ordem);
    }
}
