package com.weekit.pix.pagador.pix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.weekit.pix.pagador.pix.PixException.Motivo.SALDO_INSUFICIENTE;

/** Conta do nosso único cliente, o João. Em memória, para o código ficar curto. */
@Component
public class ContaCorrente {

    private static final Logger log = LoggerFactory.getLogger(ContaCorrente.class);

    private final String titular = "Joao Silva";
    private final String agencia = "0001";
    private final String numero = "11111-1";
    private BigDecimal saldo = new BigDecimal("2000.00");

    public synchronized void debitar(BigDecimal valor) {
        if (saldo.compareTo(valor) < 0) {
            throw new PixException(SALDO_INSUFICIENTE, "Saldo insuficiente. Saldo atual: R$ " + saldo);
        }
        saldo = saldo.subtract(valor);
        log.info("[CONTA] debitado R$ {} de {}. Saldo: R$ {}", valor, titular, saldo);
    }

    public synchronized void estornar(BigDecimal valor) {
        saldo = saldo.add(valor);
        log.warn("[CONTA] estornado R$ {} para {}. Saldo: R$ {}", valor, titular, saldo);
    }

    public synchronized BigDecimal saldo() {
        return saldo;
    }

    public String titular() {
        return titular;
    }

    public String agencia() {
        return agencia;
    }

    public String numero() {
        return numero;
    }
}
