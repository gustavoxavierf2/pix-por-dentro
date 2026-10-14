package com.weekit.pix.recebedor.api;

import com.weekit.pix.recebedor.pix.ContasBancarias;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/** Só para a demo: deixa ver os saldos dos clientes do Banco Bahia. */
@RestController
public class ContaController {

    private final ContasBancarias contas;

    public ContaController(ContasBancarias contas) {
        this.contas = contas;
    }

    @GetMapping("/contas")
    public Map<String, BigDecimal> contas() {
        return contas.saldos();
    }
}
