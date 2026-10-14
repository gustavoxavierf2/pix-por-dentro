package com.weekit.pix.pagador.pix;

import com.weekit.pix.pagador.Bancos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * DICT simulado — Diretório de Identificadores de Contas Transacionais.
 *
 * É o "catálogo" do Banco Central que traduz uma chave Pix (e-mail, celular, CPF, aleatória)
 * em dados bancários: qual banco, agência, conta e nome do titular.
 * É por causa dele que o app mostra "Maria Souza - Banco Bahia" antes de você confirmar.
 *
 * No Pix real é uma API do Banco Central consultada por HTTPS. Aqui é um Map em memória.
 */
@Component
public class Dict {

    private static final Logger log = LoggerFactory.getLogger(Dict.class);

    public record Vinculo(String ispb, String banco, String agencia, String conta, String nome) {
    }

    private final Map<String, Vinculo> chaves = Map.of(
            // Cenário feliz
            "maria@email.com", bahia("12345-6", "Maria Souza"),
            "+5577999990000", bahia("55555-5", "Pedro Lima"),
            // Chave ainda cadastrada, mas a conta foi encerrada no banco recebedor -> rejeição + estorno
            "ana@email.com", bahia("99999-9", "Ana Antiga"),
            // Conta que faz o sistema do recebedor quebrar -> retentativas + DLQ
            "bug@email.com", bahia("00000-0", "Conta Bugada")
    );

    public Optional<Vinculo> consultar(String chave) {
        var vinculo = Optional.ofNullable(chaves.get(chave));
        log.info("[DICT] consulta chave '{}' -> {}", chave,
                vinculo.map(v -> v.nome() + " | " + v.banco() + " ag " + v.agencia() + " cc " + v.conta())
                        .orElse("NAO ENCONTRADA"));
        return vinculo;
    }

    private static Vinculo bahia(String conta, String nome) {
        return new Vinculo(Bancos.ISPB_RECEBEDOR, Bancos.NOME_RECEBEDOR, "0001", conta, nome);
    }
}
