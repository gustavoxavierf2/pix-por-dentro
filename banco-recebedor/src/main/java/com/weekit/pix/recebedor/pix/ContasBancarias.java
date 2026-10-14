package com.weekit.pix.recebedor.pix;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Contas dos clientes do Banco Bahia, em memória.
 * A 99999-9 (Ana) não existe mais: foi encerrada.
 */
@Component
public class ContasBancarias {

    private final Map<String, BigDecimal> saldos = new ConcurrentSkipListMap<>(Map.of(
            "12345-6", BigDecimal.ZERO, // Maria Souza
            "55555-5", BigDecimal.ZERO, // Pedro Lima
            "00000-0", BigDecimal.ZERO  // Conta Bugada
    ));

    public boolean existe(String conta) {
        return saldos.containsKey(conta);
    }

    /** Devolve o novo saldo. */
    public BigDecimal creditar(String conta, BigDecimal valor) {
        return saldos.merge(conta, valor, BigDecimal::add);
    }

    public Map<String, BigDecimal> saldos() {
        return Collections.unmodifiableMap(saldos);
    }
}
