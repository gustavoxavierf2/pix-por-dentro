package com.weekit.pix.pagador.pix;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** O "RG" único de um Pix: identifica a transação do começo ao fim, em todos os sistemas. */
public final class EndToEndId {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);
    private static final String ALFANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private EndToEndId() {
    }

    /** Formato oficial: E + ISPB (8) + data/hora UTC yyyyMMddHHmm (12) + 11 caracteres aleatórios = 32. */
    public static String gerar(String ispb) {
        var sufixo = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sufixo.append(ALFANUM.charAt(RANDOM.nextInt(ALFANUM.length())));
        }
        return "E" + ispb + DATA.format(Instant.now()) + sufixo;
    }
}
