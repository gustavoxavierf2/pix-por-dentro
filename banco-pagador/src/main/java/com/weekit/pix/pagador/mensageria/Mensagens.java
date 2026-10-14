package com.weekit.pix.pagador.mensageria;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * As mensagens que os bancos trocam pelo SPI.
 * No Pix real elas seguem o padrão internacional ISO 20022 (em XML).
 * Aqui é uma versão simplificada em JSON, com os mesmos nomes.
 */
public final class Mensagens {

    private Mensagens() {
    }

    /** Um lado da transação: banco (ISPB), agência, conta e titular. */
    public record Participante(String ispb, String agencia, String conta, String nome) {
    }

    /**
     * pacs.008 — "ordem de pagamento": banco do pagador avisa o banco do recebedor
     * que é para creditar um valor numa conta.
     */
    public record Pacs008(
            String endToEndId,
            BigDecimal valor,
            String chave,
            Participante pagador,
            Participante recebedor,
            String mensagem,
            Instant criadoEm) {
    }

    /**
     * pacs.002 — resposta do banco recebedor.
     * status ACSC = aceito e creditado | RJCT = rejeitado (motivo explica por quê)
     */
    public record Pacs002(String endToEndId, String status, String motivo) {

        private static final String ACEITO = "ACSC";
        private static final String REJEITADO = "RJCT";

        public static Pacs002 aceito(String endToEndId) {
            return new Pacs002(endToEndId, ACEITO, null);
        }

        public static Pacs002 rejeitado(String endToEndId, String motivo) {
            return new Pacs002(endToEndId, REJEITADO, motivo);
        }

        public boolean foiAceito() {
            return ACEITO.equals(status);
        }
    }
}
