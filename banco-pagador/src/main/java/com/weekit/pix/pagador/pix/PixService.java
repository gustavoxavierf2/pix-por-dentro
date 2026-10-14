package com.weekit.pix.pagador.pix;

import com.weekit.pix.pagador.Bancos;
import com.weekit.pix.pagador.mensageria.Mensagens.Pacs002;
import com.weekit.pix.pagador.mensageria.Mensagens.Pacs008;
import com.weekit.pix.pagador.mensageria.Mensagens.Participante;
import com.weekit.pix.pagador.mensageria.SpiIndisponivelException;
import com.weekit.pix.pagador.mensageria.SpiPublisher;
import com.weekit.pix.pagador.pix.Transacao.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;

import static com.weekit.pix.pagador.pix.PixException.Motivo.CHAVE_NAO_ENCONTRADA;
import static com.weekit.pix.pagador.pix.PixException.Motivo.TRANSACAO_NAO_ENCONTRADA;

/**
 * Banco do PAGADOR (Banco Conquista). O cliente João Silva quer mandar um Pix.
 * Esta classe só conduz o fluxo; cada passo é feito por uma classe especializada.
 *
 * Passo a passo de um Pix, do lado de quem paga:
 *   1. Consulta a chave no DICT          -> descobre para quem vai o dinheiro
 *   2. Antifraude e limites              -> pode barrar antes de sair
 *   3. Debita a conta do pagador         -> o dinheiro sai na hora
 *   4. Gera o endToEndId                 -> o "RG" único da transação
 *   5. Envia a pacs.008 para o SPI       -> e responde ao app: "em processamento"
 *   6. Recebe a pacs.002 do recebedor    -> conclui, ou estorna se foi rejeitado
 */
@Service
public class PixService {

    private static final Logger log = LoggerFactory.getLogger(PixService.class);

    private final Dict dict;
    private final Antifraude antifraude;
    private final ContaCorrente conta;
    private final TransacaoRepository transacoes;
    private final SpiPublisher spi;

    public PixService(Dict dict, Antifraude antifraude, ContaCorrente conta,
                      TransacaoRepository transacoes, SpiPublisher spi) {
        this.dict = dict;
        this.antifraude = antifraude;
        this.conta = conta;
        this.transacoes = transacoes;
        this.spi = spi;
    }

    // synchronized: a resposta do SPI pode chegar antes de este método terminar de gravar a transação
    public synchronized Transacao enviar(String chave, BigDecimal valor, String mensagem) {
        log.info("==================== NOVO PIX: R$ {} para '{}' ====================", valor, chave);

        // 1. DICT: de quem é essa chave?
        var destino = dict.consultar(chave)
                .orElseThrow(() -> new PixException(CHAVE_NAO_ENCONTRADA, "Chave Pix nao encontrada no DICT"));

        // 2. Antifraude e limites
        antifraude.verificar(valor);

        // 3. Débito: o dinheiro sai da conta do pagador antes de saber se o recebedor aceitou
        conta.debitar(valor);

        // 4. endToEndId + ordem de pagamento (pacs.008)
        var pagador = new Participante(Bancos.ISPB_PAGADOR, conta.agencia(), conta.numero(), conta.titular());
        var recebedor = new Participante(destino.ispb(), destino.agencia(), destino.conta(), destino.nome());
        var pacs008 = new Pacs008(EndToEndId.gerar(Bancos.ISPB_PAGADOR), valor, chave,
                pagador, recebedor, mensagem, Instant.now());

        // 5. Envia para o SPI e NÃO espera o recebedor (assíncrono)
        try {
            spi.enviarOrdemDePagamento(pacs008);
        } catch (SpiIndisponivelException e) {
            log.error("[SPI] fora do ar! Estornando R$ {}", valor);
            conta.estornar(valor);
            throw e;
        }

        var transacao = new Transacao(pacs008.endToEndId(), destino.nome(), destino.banco(), valor,
                Status.EM_PROCESSAMENTO, null, pacs008);
        transacoes.salvar(transacao);
        return transacao;
    }

    /** 6. Resposta do banco recebedor chegou pelo SPI. */
    public synchronized void processarResposta(Pacs002 resposta) {
        var transacao = transacoes.buscar(resposta.endToEndId()).orElse(null);

        if (transacao == null || transacao.status() != Status.EM_PROCESSAMENTO) {
            log.warn("[SPI] resposta repetida ou desconhecida, ignorando (idempotencia)");
            return;
        }

        if (resposta.foiAceito()) {
            transacoes.salvar(transacao.comStatus(Status.CONCLUIDO, null));
            log.info("[APP] Pix de R$ {} para {} CONCLUIDO", transacao.valor(), transacao.para());
        } else {
            // Recebedor rejeitou: devolve o dinheiro para o pagador
            log.warn("[APP] Pix de R$ {} para {} REJEITADO: {}", transacao.valor(), transacao.para(), resposta.motivo());
            conta.estornar(transacao.valor());
            transacoes.salvar(transacao.comStatus(Status.REJEITADO, resposta.motivo()));
        }
    }

    /** Simula uma falha de rede em que o banco reenvia a mesma mensagem. */
    public Transacao reenviar(String endToEndId) {
        var transacao = buscar(endToEndId);
        log.warn("[SPI] REENVIANDO a mesma pacs.008 {} (simulando falha de rede)", endToEndId);
        spi.enviarOrdemDePagamento(transacao.mensagemEnviada());
        return transacao;
    }

    public Transacao buscar(String endToEndId) {
        return transacoes.buscar(endToEndId)
                .orElseThrow(() -> new PixException(TRANSACAO_NAO_ENCONTRADA, "Transacao nao encontrada"));
    }

    public Collection<Transacao> listar() {
        return transacoes.listar();
    }
}
