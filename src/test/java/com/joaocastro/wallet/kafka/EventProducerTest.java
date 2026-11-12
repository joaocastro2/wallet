package com.joaocastro.wallet.kafka;

import com.joaocastro.wallet.event.AssetPriceUpdatedEvent;
import com.joaocastro.wallet.event.TransactionCreatedEvent;
import com.joaocastro.wallet.kafka.producer.EventProducer;
import com.joaocastro.wallet.model.enums.TransactionType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks
    private EventProducer producer;

    @Test
    void devePublicarEventoDePrecoComSimboloComoChave() {
        AssetPriceUpdatedEvent event = new AssetPriceUpdatedEvent("PETR4", new BigDecimal("40"), LocalDateTime.now());
        given(kafkaTemplate.send(EventProducer.TOPIC_ASSET_PRICE_UPDATED, "PETR4", event))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("broker indisponivel")));

        producer.sendAssetPriceUpdatedEvent(event);

        verify(kafkaTemplate).send(EventProducer.TOPIC_ASSET_PRICE_UPDATED, "PETR4", event);
    }

    @Test
    void devePublicarEventoDeTransacaoComAtivoComoChave() {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), "PETR4", TransactionType.BUY, BigDecimal.ONE,
                new BigDecimal("40"), new BigDecimal("40"), LocalDateTime.now());
        given(kafkaTemplate.send(EventProducer.TOPIC_TRANSACTION_CREATED, "PETR4", event))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("broker indisponivel")));

        producer.sendTransactionCreatedEvent(event);

        verify(kafkaTemplate).send(EventProducer.TOPIC_TRANSACTION_CREATED, "PETR4", event);
    }
}
