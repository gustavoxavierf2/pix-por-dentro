package com.weekit.pix.recebedor.pix;

import com.weekit.pix.recebedor.mensageria.Mensagens.Pacs008;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Avisa o cliente no celular. Aqui só escreve no log. */
@Component
public class NotificacaoPush {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPush.class);

    public void avisarRecebimento(Pacs008 ordem) {
        log.info("[NOTIFICACAO] {}, voce recebeu um Pix de R$ {} de {}{}",
                ordem.recebedor().nome(), ordem.valor(), ordem.pagador().nome(),
                ordem.mensagem() == null ? "" : ": \"" + ordem.mensagem() + "\"");
    }
}
