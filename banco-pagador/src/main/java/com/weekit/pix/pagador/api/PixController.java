package com.weekit.pix.pagador.api;

import com.weekit.pix.pagador.pix.PixService;
import com.weekit.pix.pagador.pix.Transacao;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/** O "app do banco" do João. Só traduz HTTP; as regras ficam no PixService. */
@RestController
public class PixController {

    private final PixService pixService;

    public PixController(PixService pixService) {
        this.pixService = pixService;
    }

    /** 202 Accepted = "recebi seu pedido e estou processando", não "já terminou". */
    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Transacao enviar(@RequestBody PixRequest request) {
        return pixService.enviar(request.chave(), request.valor(), request.mensagem());
    }

    @GetMapping("/pix")
    public Collection<Transacao> listar() {
        return pixService.listar();
    }

    @GetMapping("/pix/{endToEndId}")
    public Transacao buscar(@PathVariable String endToEndId) {
        return pixService.buscar(endToEndId);
    }

    @PostMapping("/pix/{endToEndId}/reenviar")
    public Transacao reenviar(@PathVariable String endToEndId) {
        return pixService.reenviar(endToEndId);
    }
}
