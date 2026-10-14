package com.weekit.pix.pagador.pix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;

import static com.weekit.pix.pagador.pix.PixException.Motivo.LIMITE_EXCEDIDO;
import static com.weekit.pix.pagador.pix.PixException.Motivo.VALOR_INVALIDO;

/** Antifraude e limites. Aqui só o limite noturno; banco real usa dezenas de regras e modelos. */
@Component
public class Antifraude {

    private static final Logger log = LoggerFactory.getLogger(Antifraude.class);
    private static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private final BigDecimal limiteNoturno;
    private final boolean forcarHorarioNoturno;

    public Antifraude(@Value("${pix.limite-noturno}") BigDecimal limiteNoturno,
                      @Value("${pix.forcar-horario-noturno}") boolean forcarHorarioNoturno) {
        this.limiteNoturno = limiteNoturno;
        this.forcarHorarioNoturno = forcarHorarioNoturno;
    }

    public void verificar(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new PixException(VALOR_INVALIDO, "Valor deve ser maior que zero");
        }
        if (horarioNoturno() && valor.compareTo(limiteNoturno) > 0) {
            log.warn("[ANTIFRAUDE] BLOQUEADO: R$ {} acima do limite noturno de R$ {}", valor, limiteNoturno);
            throw new PixException(LIMITE_EXCEDIDO, "Limite noturno de R$ " + limiteNoturno + " excedido (20h-6h)");
        }
        log.info("[ANTIFRAUDE] ok");
    }

    private boolean horarioNoturno() {
        var agora = LocalTime.now(BRASIL);
        return forcarHorarioNoturno || agora.isAfter(LocalTime.of(20, 0)) || agora.isBefore(LocalTime.of(6, 0));
    }
}
