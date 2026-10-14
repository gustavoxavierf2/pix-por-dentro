package com.weekit.pix.pagador.pix;

/** Pix barrado por uma regra de negócio. A camada HTTP decide qual status devolver. */
public class PixException extends RuntimeException {

    public enum Motivo {
        CHAVE_NAO_ENCONTRADA,
        VALOR_INVALIDO,
        LIMITE_EXCEDIDO,
        SALDO_INSUFICIENTE,
        TRANSACAO_NAO_ENCONTRADA
    }

    private final Motivo motivo;

    public PixException(Motivo motivo, String mensagem) {
        super(mensagem);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
