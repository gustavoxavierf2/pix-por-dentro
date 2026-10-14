package com.weekit.pix.recebedor.pix;

import com.weekit.pix.recebedor.mensageria.Mensagens.Pacs002;
import com.weekit.pix.recebedor.mensageria.Mensagens.Pacs008;
import com.weekit.pix.recebedor.mensageria.SpiPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Banco do RECEBEDOR (Banco Bahia).
 * Esta classe só conduz o fluxo; cada passo é feito por uma classe especializada.
 *
 * Passo a passo de um Pix, do lado de quem recebe:
 *   1. Recebe a pacs.008 pelo SPI        -> (feito pelo SpiListener)
 *   2. Já processei esse endToEndId?     -> se sim, ignora (idempotência: nunca credita duas vezes)
 *   3. A conta existe e está ativa?      -> se não, responde RJCT
 *   4. Credita a conta do recebedor
 *   5. Responde pacs.002 ACSC pelo SPI
 *   6. Notifica o cliente no celular
 */
@Service
public class RecebimentoService {

    private static final Logger log = LoggerFactory.getLogger(RecebimentoService.class);

    /** Demo: conta que simula um bug no core bancário (retentativas + DLQ). */
    private static final String CONTA_COM_BUG = "00000-0";

    private final ContasBancarias contas;
    private final OrdensProcessadas processadas;
    private final SpiPublisher spi;
    private final NotificacaoPush notificacao;

    public RecebimentoService(ContasBancarias contas, OrdensProcessadas processadas,
                              SpiPublisher spi, NotificacaoPush notificacao) {
        this.contas = contas;
        this.processadas = processadas;
        this.spi = spi;
        this.notificacao = notificacao;
    }

    // synchronized: "já processei?" e "registrar" precisam acontecer juntos, senão duas mensagens iguais passam
    public synchronized void receber(Pacs008 ordem) {
        var endToEndId = ordem.endToEndId();

        // 2. Idempotência: a mesma mensagem pode chegar mais de uma vez (rede, retentativa, reenvio)
        if (processadas.jaProcessada(endToEndId)) {
            log.warn("[IDEMPOTENCIA] {} ja foi processado. Ignorando, NAO credita de novo.", endToEndId);
            return;
        }

        var conta = ordem.recebedor().conta();

        // Simula um bug no nosso sistema: a exceção faz o RabbitMQ tentar de novo e, depois, mandar para a DLQ
        if (CONTA_COM_BUG.equals(conta)) {
            log.error("[BUG] erro inesperado ao processar {}. O RabbitMQ vai tentar de novo (ate 3x).", endToEndId);
            throw new IllegalStateException("Falha inesperada no core bancario");
        }

        // 3. Conta existe?
        if (!contas.existe(conta)) {
            processadas.registrar(endToEndId);
            log.warn("[CONTA] conta {} inexistente ou encerrada. Respondendo RJCT.", conta);
            spi.responder(ordem, Pacs002.rejeitado(endToEndId, "AC03 - Conta do recebedor invalida ou encerrada"));
            return;
        }

        // 4. Crédito
        var novoSaldo = contas.creditar(conta, ordem.valor());
        processadas.registrar(endToEndId);
        log.info("[CONTA] creditado R$ {} na conta {}. Saldo: R$ {}", ordem.valor(), conta, novoSaldo);

        // 5. Confirma para o banco do pagador
        spi.responder(ordem, Pacs002.aceito(endToEndId));

        // 6. Notificação push
        notificacao.avisarRecebimento(ordem);
    }
}
