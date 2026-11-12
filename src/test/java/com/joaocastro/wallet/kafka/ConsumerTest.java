package com.joaocastro.wallet.kafka;

import com.joaocastro.wallet.event.AssetPriceUpdatedEvent;
import com.joaocastro.wallet.event.TransactionCreatedEvent;
import com.joaocastro.wallet.event.UserCreatedEvent;
import com.joaocastro.wallet.kafka.consumer.EventConsumer;
import com.joaocastro.wallet.kafka.consumer.UserConsumer;
import com.joaocastro.wallet.model.enums.TransactionType;
import com.joaocastro.wallet.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {

    @Mock
    private PortfolioService portfolioService;
    @InjectMocks
    private EventConsumer eventConsumer;
    @InjectMocks
    private UserConsumer userConsumer;

    @Test
    void deveEncaminharEventoDeTransacaoAoPortfolio() {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), "PETR4", TransactionType.BUY, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, LocalDateTime.now());

        eventConsumer.consumeTransactionCreated(event);

        verify(portfolioService).processTransactionEvent(event);
    }

    @Test
    void deveConsumirEventoDeTransacaoMesmoQuandoPortfolioFalhar() {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), "PETR4", TransactionType.SELL, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, LocalDateTime.now());
        doThrow(new IllegalArgumentException("ativo invalido"))
                .when(portfolioService).processTransactionEvent(event);

        eventConsumer.consumeTransactionCreated(event);

        verify(portfolioService).processTransactionEvent(event);
    }

    @Test
    void deveAceitarAtualizacaoDePrecoSemProcessarCarteira() {
        eventConsumer.consumeAssetPriceUpdated(
                new AssetPriceUpdatedEvent("PETR4", BigDecimal.TEN, LocalDateTime.now()));

        verifyNoInteractions(portfolioService);
    }

    @Test
    void deveEncaminharEventoDeUsuarioAoPortfolio() {
        UserCreatedEvent event = new UserCreatedEvent(
                UUID.randomUUID(), "Joao", "joao@email.com", LocalDateTime.now());

        userConsumer.consumeUserCreated(event);

        verify(portfolioService).processUserCreatedEvent(event);
    }
}
