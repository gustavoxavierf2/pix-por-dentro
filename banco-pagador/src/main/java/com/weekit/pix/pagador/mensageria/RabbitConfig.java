package com.weekit.pix.pagador.mensageria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekit.pix.pagador.Bancos;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A "planta" do SPI: exchanges, filas e bindings.
 *
 * Aqui o RabbitMQ faz o papel do SPI (Sistema de Pagamentos Instantâneos do Banco Central).
 * No Pix real, o SPI não é um RabbitMQ: os bancos trocam mensagens XML (ISO 20022)
 * com o Banco Central pela rede RSFN. Mas a ideia é a mesma: ninguém chama o outro
 * banco diretamente, todo mundo manda mensagem para o "carteiro" central.
 *
 * Esta mesma configuração existe nos dois bancos, para que tanto faz quem sobe primeiro.
 */
@Configuration
public class RabbitConfig {

    public static final String SPI = "spi";
    public static final String SPI_DLX = "spi.dlx";

    // Caixa de entrada do banco recebedor: ordens de pagamento (pacs.008)
    public static final String FILA_PACS008_RECEBEDOR = "spi." + Bancos.ISPB_RECEBEDOR + ".pacs008";
    public static final String FILA_PACS008_RECEBEDOR_DLQ = FILA_PACS008_RECEBEDOR + ".dlq";
    // Caixa de entrada do banco pagador: respostas (pacs.002)
    public static final String FILA_PACS002_PAGADOR = "spi." + Bancos.ISPB_PAGADOR + ".pacs002";

    /** Routing key = "endereço" da mensagem: o exchange usa para escolher a fila de destino. */
    public static String chavePacs008(String ispbRecebedor) {
        return ispbRecebedor + ".pacs008";
    }

    public static String chavePacs002(String ispbPagador) {
        return ispbPagador + ".pacs002";
    }

    // ---- Exchanges: recebem as mensagens e decidem para qual fila vão ----

    @Bean
    DirectExchange spi() {
        return new DirectExchange(SPI);
    }

    @Bean
    DirectExchange spiDlx() {
        return new DirectExchange(SPI_DLX);
    }

    // ---- Filas: onde as mensagens esperam até alguém consumir ----

    @Bean
    Queue filaPacs008Recebedor() {
        // Mensagem que falhar várias vezes vai para a DLQ em vez de travar a fila
        return QueueBuilder.durable(FILA_PACS008_RECEBEDOR)
                .deadLetterExchange(SPI_DLX)
                .deadLetterRoutingKey(FILA_PACS008_RECEBEDOR_DLQ)
                .build();
    }

    @Bean
    Queue filaPacs008RecebedorDlq() {
        return QueueBuilder.durable(FILA_PACS008_RECEBEDOR_DLQ).build();
    }

    @Bean
    Queue filaPacs002Pagador() {
        return QueueBuilder.durable(FILA_PACS002_PAGADOR).build();
    }

    // ---- Bindings: a ligação "exchange + routing key -> fila" ----

    @Bean
    Binding bindingPacs008() {
        return BindingBuilder.bind(filaPacs008Recebedor()).to(spi()).with(chavePacs008(Bancos.ISPB_RECEBEDOR));
    }

    @Bean
    Binding bindingPacs002() {
        return BindingBuilder.bind(filaPacs002Pagador()).to(spi()).with(chavePacs002(Bancos.ISPB_PAGADOR));
    }

    @Bean
    Binding bindingDlq() {
        return BindingBuilder.bind(filaPacs008RecebedorDlq()).to(spiDlx()).with(FILA_PACS008_RECEBEDOR_DLQ);
    }

    // ---- Formato das mensagens: JSON ----

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        var converter = new Jackson2JsonMessageConverter(objectMapper);
        // Usa o tipo do parâmetro do @RabbitListener (os dois bancos têm pacotes diferentes)
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
