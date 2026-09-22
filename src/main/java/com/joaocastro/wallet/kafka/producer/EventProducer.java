package com.joaocastro.wallet.kafka.producer;

import com.joaocastro.wallet.event.AssetPriceUpdatedEvent;
import com.joaocastro.wallet.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    // Constantes com os nomes dos tópicos no Kafka
    public static final String TOPIC_ASSET_PRICE_UPDATED = "asset-price-updated-topic";
    public static final String TOPIC_TRANSACTION_CREATED = "transaction-created-topic";

    // KafkaTemplate fornecido e autowired pelo Spring Boot
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Envia um evento de atualização de preço para o Kafka.
     * Usa o 'symbol' do ativo como chave (Key) para garantir a ordenação de mensagens por ativo.
     */
    public void sendAssetPriceUpdatedEvent(AssetPriceUpdatedEvent event) {
        log.info("Publicando evento no Kafka [{}]: simbolo={}, novoPreco={}",
                TOPIC_ASSET_PRICE_UPDATED, event.symbol(), event.newPrice());

        kafkaTemplate.send(TOPIC_ASSET_PRICE_UPDATED, event.symbol(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Evento publicado com sucesso no topico [{}] | Particao: {} | Offset: {}",
                                TOPIC_ASSET_PRICE_UPDATED,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Falha ao publicar evento no topico [{}] para o ativo: {}",
                                TOPIC_ASSET_PRICE_UPDATED, event.symbol(), ex);
                    }
                });
    }

    /**
     * Envia um evento de transação criada (compra ou venda) para o Kafka.
     */
    public void sendTransactionCreatedEvent(TransactionCreatedEvent event) {
        log.info("Publicando evento no Kafka [{}]: id={}, tipo={}, simbolo={}",
                TOPIC_TRANSACTION_CREATED, event.transactionId(), event.type(), event.assetSymbol());

        kafkaTemplate.send(TOPIC_TRANSACTION_CREATED, event.assetSymbol(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Evento publicado com sucesso no topico [{}] | Particao: {} | Offset: {}",
                                TOPIC_TRANSACTION_CREATED,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Falha ao publicar evento no topico [{}] para a transacao: {}",
                                TOPIC_TRANSACTION_CREATED, event.transactionId(), ex);
                    }
                });
    }
}