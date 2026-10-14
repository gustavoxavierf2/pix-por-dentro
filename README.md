# Por Dentro de um Pix — demo do minicurso (Week-IT)

Dois bancos fictícios trocando um Pix, com o RabbitMQ no papel do SPI do Banco Central.

```
  App do João                                                     Celular da Maria
      │ POST /pix                                                        ▲
      ▼                                                                  │ notificação
┌──────────────────┐   pacs.008 (ordem de pagamento)   ┌──────────────────┐
│ BANCO CONQUISTA  │ ────────────►  ┌────────┐ ───────► │   BANCO BAHIA    │
│ (pagador, 8080)  │                │  SPI   │          │ (recebedor, 8081)│
│ 1. consulta DICT │ ◄────────────  │RabbitMQ│ ◄─────── │ 2. idempotência  │
│ 2. antifraude    │   pacs.002 (ACSC aceito / RJCT)    │ 3. conta existe? │
│ 3. debita        │                └────────┘          │ 4. credita       │
│ 4. endToEndId    │                                    │ 5. responde      │
│ 5. publica       │                                    │ 6. notifica      │
│ 6. conclui ou    │                                    └──────────────────┘
│    estorna       │
└──────────────────┘
```

## Como rodar

Precisa de Java 17+, Maven e Docker.

```bash
docker compose up -d                          # sobe o "SPI" (RabbitMQ)
cd banco-recebedor && mvn spring-boot:run     # terminal 1
cd banco-pagador   && mvn spring-boot:run     # terminal 2
```

Painel do RabbitMQ: http://localhost:15672 (guest / guest), aba **Queues**.

Deixe os dois terminais lado a lado no projetor: os logs contam a história, com tags como `[DICT]`, `[ANTIFRAUDE]`, `[SPI]`, `[CONTA]` e `[NOTIFICACAO]`.

No IntelliJ, as configurações **Demo Pix completa**, **Banco Pagador** e **Banco Recebedor** já estão prontas (pasta `.idea/runConfigurations`).

As requisições da demo estão em `demo.http` (HTTP Client do IntelliJ) e em `pix-por-dentro.postman_collection.json` (Postman: Import). As duas têm os mesmos 8 cenários, mais as consultas ao RabbitMQ (filas, bindings, DLQ). Também dá para usar curl:

```bash
curl -X POST localhost:8080/pix -H "Content-Type: application/json" \
     -d '{"chave":"maria@email.com","valor":50.00,"mensagem":"Almoço do Weekit"}'
```

## Roteiro da demo

| # | Cenário | Chave | O que mostrar |
|---|---------|-------|---------------|
| 1 | Pix normal | `maria@email.com` | Resposta **202** com status `EM_PROCESSAMENTO`: o app não espera o outro banco. Segundos depois o `GET /pix/{id}` mostra `CONCLUIDO`. Mostre o `endToEndId` e a `mensagemEnviada` (é a pacs.008). |
| 2 | Chave inexistente | `ninguem@email.com` | Barrado no DICT antes de qualquer dinheiro sair (404). |
| 3 | Saldo insuficiente | `maria@email.com`, R$ 99.999 | Barrado no próprio banco (422). Entre 20h e 6h o antifraude barra antes e a mensagem é de limite noturno: ensaie de dia. |
| 4 | Limite noturno | `+5577999990000`, R$ 1.500 | Regra do BC: R$ 1.000 entre 20h e 6h. Para demonstrar de dia, use `pix.forcar-horario-noturno=true`. |
| 5 | Conta encerrada | `ana@email.com` | O DICT ainda aponta a conta, o recebedor responde **RJCT AC03** e o pagador **estorna**. Veja o saldo do João voltar. |
| 6 | Mensagem duplicada | `POST /pix/{id}/reenviar` | O recebedor reconhece o `endToEndId` e **não credita duas vezes** (idempotência). |
| 7 | Recebedor fora do ar | qualquer | Pare o banco-recebedor (Ctrl+C), mande 3 ou 4 Pix e veja a fila `spi.22222222.pacs008` encher no painel. Suba de novo e veja a fila esvaziar. Nenhum Pix se perde. (Os saldos ficam em memória, então reiniciar o recebedor zera as contas da Maria e do Pedro.) |
| 8 | Bug no recebedor | `bug@email.com` | 3 tentativas no log, depois a linha `[DLQ]` e a mensagem vai para a **DLQ** (`...pacs008.dlq`) em vez de travar a fila. Pergunta para a turma: *e o dinheiro do João?* (ficou debitado e `EM_PROCESSAMENTO`; no Pix real o SPI tem prazo de segundos e, sem resposta, a transação é rejeitada e o valor volta). |

## Simulação x Pix real

| Aqui | No Pix real |
|------|-------------|
| `Dict` é um `Map` em memória | DICT é uma API do Banco Central, consultada por HTTPS |
| RabbitMQ faz o papel do SPI | SPI é o sistema do BC, acessado pela RSFN (rede do sistema financeiro) |
| Mensagens em JSON | Mensagens XML no padrão ISO 20022 (pacs.008, pacs.002) |
| Banco recebedor credita e avisa o pagador direto | O SPI liquida o valor entre as contas PI dos bancos no BC e confirma para os dois |
| Antifraude = limite noturno | Dezenas de regras, modelos de ML, MED (Mecanismo Especial de Devolução) |
| Dados em memória | Banco de dados, auditoria e alta disponibilidade |

## Estrutura

Cada banco tem três pacotes. Por fora entram dois tipos de pedido: HTTP (`api`) e mensagem (`SpiListener`). Os dois só repassam para o `pix`, onde ficam as regras, e quem precisa enviar mensagem usa o `SpiPublisher`.

```
banco-pagador/
  api/         PixController, ContaController (HTTP), ApiExceptionHandler (erro de negócio vira status HTTP)
  pix/         PixService (conduz o fluxo), Dict, Antifraude, ContaCorrente, TransacaoRepository, EndToEndId
  mensageria/  RabbitConfig (exchanges, filas, bindings), SpiPublisher (envia), SpiListener (recebe), Mensagens

banco-recebedor/
  api/         ContaController (só para ver os saldos na demo)
  pix/         RecebimentoService (conduz o fluxo), ContasBancarias, OrdensProcessadas (idempotência), NotificacaoPush
  mensageria/  RabbitConfig, SpiListener (recebe), SpiPublisher (responde), DlqConfig (retentativa e DLQ), Mensagens
```

O Spring AMQP só aparece no pacote `mensageria`. Os pacotes `pix` e `api` não importam nada dele, o que deixa a mensageria fácil de apontar na apresentação.

Cada banco tem uma cópia de `RabbitConfig` e `Mensagens`. Na vida real, cada banco tem o seu sistema e só o formato da mensagem é combinado.

Para montar a apresentação (ordem dos assuntos, ordem dos cenários, o que dizer em cada um), veja [APRESENTACAO.md](APRESENTACAO.md).
