package com.weekit.pix.recebedor.pix;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memória da IDEMPOTÊNCIA: quais endToEndId este banco já tratou.
 * Como a mesma mensagem pode chegar mais de uma vez, sem isso o cliente seria creditado duas vezes.
 * No banco real seria uma tabela com chave única.
 */
@Component
public class OrdensProcessadas {

    private final Set<String> endToEndIds = ConcurrentHashMap.newKeySet();

    public boolean jaProcessada(String endToEndId) {
        return endToEndIds.contains(endToEndId);
    }

    public void registrar(String endToEndId) {
        endToEndIds.add(endToEndId);
    }
}
