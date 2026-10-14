package com.weekit.pix.pagador.pix;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Guarda os Pix do João em memória. No banco real seria uma tabela. */
@Repository
public class TransacaoRepository {

    private final Map<String, Transacao> transacoes = new ConcurrentHashMap<>();

    public void salvar(Transacao transacao) {
        transacoes.put(transacao.endToEndId(), transacao);
    }

    public Optional<Transacao> buscar(String endToEndId) {
        return Optional.ofNullable(transacoes.get(endToEndId));
    }

    public Collection<Transacao> listar() {
        return transacoes.values();
    }
}
