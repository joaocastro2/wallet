package com.joaocastro.wallet.kafka.consumer;

import com.joaocastro.wallet.event.UserCreatedEvent;
import com.joaocastro.wallet.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserConsumer {

    private final PortfolioService portfolioService;

    @KafkaListener(topics = "user-created-topic", groupId = "portfolio-group")
    public void consumeUserCreated(UserCreatedEvent event){
        log.info("Evento recebido do kafka para criação de carteira. UserId: {}", event.userId());
        portfolioService.processUserCreatedEvent(event);
    }
}
//test
