package com.joaocastro.wallet.kafka.consumer;

import com.joaocastro.wallet.event.AssetPriceUpdatedEvent;
import com.joaocastro.wallet.event.TransactionCreatedEvent;
import com.joaocastro.wallet.kafka.producer.EventProducer;
import com.joaocastro.wallet.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final PortfolioService portfolioService;

    /**
     * Ouve o tópico de transações criadas e atualiza a carteira assincronamente.
     */
    @KafkaListener(
            topics = EventProducer.TOPIC_TRANSACTION_CREATED,
            groupId = "wallet-group"
    )
    public void consumeTransactionCreated(TransactionCreatedEvent event) {
        log.info("Evento recebido do Kafka [{}]: idTransacao={}, tipo={}, simbolo={}",
                EventProducer.TOPIC_TRANSACTION_CREATED, event.transactionId(), event.type(), event.assetSymbol());

        try {
            portfolioService.processTransactionEvent(event);
        } catch (Exception e) {
            log.error("Erro ao processar transacao recebida do Kafka para o ativo: {}", event.assetSymbol(), e);
        }
    }

    /**
     * Ouve o tópico de preços de ativos atualizados.
     */
    @KafkaListener(
            topics = EventProducer.TOPIC_ASSET_PRICE_UPDATED,
            groupId = "wallet-group"
    )
    public void consumeAssetPriceUpdated(AssetPriceUpdatedEvent event) {
        log.info("Evento recebido do Kafka [{}]: simbolo={}, novoPreco={}",
                EventProducer.TOPIC_ASSET_PRICE_UPDATED, event.symbol(), event.newPrice());

        // Como o PortfolioService recalcula os totais dinamicamente no GET /wallet
        // cruzando wallet_position com asset, a simples atualização do asset.currentPrice
        // no banco já faz a carteira refletir o valor novo na próxima consulta!
    }
}