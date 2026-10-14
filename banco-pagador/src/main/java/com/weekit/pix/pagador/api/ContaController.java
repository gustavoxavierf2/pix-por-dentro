package com.weekit.pix.pagador.api;

import com.weekit.pix.pagador.Bancos;
import com.weekit.pix.pagador.pix.ContaCorrente;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class ContaController {

    public record ContaResponse(String titular, String banco, String conta, BigDecimal saldo) {
    }

    private final ContaCorrente conta;

    public ContaController(ContaCorrente conta) {
        this.conta = conta;
    }

    @GetMapping("/conta")
    public ContaResponse conta() {
        return new ContaResponse(conta.titular(), Bancos.NOME_PAGADOR, conta.numero(), conta.saldo());
    }
}
