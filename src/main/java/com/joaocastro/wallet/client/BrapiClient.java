package com.joaocastro.wallet.client;

import com.joaocastro.wallet.client.dto.BrapiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Slf4j
@Component
public class BrapiClient {

    private final WebClient webClient;

    @Value("${brapi.api.token:}")
    private String apiToken;

    public BrapiClient(WebClient.Builder webClientBuilder,
                       @Value("${brapi.api.base-url:https://brapi.dev/api}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public BigDecimal fetchCurrentPrice(String symbol) {
        try {
            BrapiResponseDto response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote/{symbol}")
                            .queryParam("token", apiToken)
                            .build(symbol))
                    .retrieve()
                    .bodyToMono(BrapiResponseDto.class)
                    .block(); // Chamada síncrona para integração com o Scheduler tradicional

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                return response.getResults().get(0).getRegularMarketPrice();
            }

            throw new RuntimeException("Cotação não encontrada na Brapi: " + symbol);
        } catch (Exception e) {
            log.error("Erro na comunicação com a Brapi: {}", e.getMessage());
            throw new RuntimeException("Erro ao buscar preço para: " + symbol, e);
        }
    }

}
