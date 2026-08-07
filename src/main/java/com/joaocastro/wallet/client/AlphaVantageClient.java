package com.joaocastro.wallet.client;

import com.joaocastro.wallet.client.dto.AlphaVantageResponseDto;
import com.joaocastro.wallet.client.dto.UsdRateResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Component
public class AlphaVantageClient {

    private final WebClient webClient;
    private final String apiKey;

    public AlphaVantageClient(
            WebClient.Builder webClientBuilder,
            @Value("${alphavantage.api.base-url:https://www.alphavantage.co}") String baseUrl,
            @Value("${ALPHA_VANTAGE_API_KEY}") String apiKey) {

        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public BigDecimal fetchCurrentPrice(String symbol, boolean isCrypto) {
        if (isCrypto) {
            return fetchCryptoPrice(symbol);
        } else {
            return fetchStockPrice(symbol);
        }
    }

    // Busca preço para Ações / FIIs
    private BigDecimal fetchStockPrice(String symbol) {
        String formattedSymbol = symbol.contains(".") ? symbol : symbol + ".SA";

        AlphaVantageResponseDto response = this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", formattedSymbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(AlphaVantageResponseDto.class)
                .block();

        if (response != null && response.globalQuote() != null && response.globalQuote().price() != null) {
            return response.globalQuote().price();
        }

        throw new RuntimeException("Não foi possível obter a cotação da ação para o símbolo: " + symbol);
    }

    // Busca preço para Criptomoedas (ex: BTC/BRL ou BTC/USD)
    private BigDecimal fetchCryptoPrice(String symbol) {
        String cleanSymbol = symbol.trim().toUpperCase().split("[./-]")[0];

        // 1. Busca o preço da criptomoeda em USD na Alpha Vantage
        AlphaVantageResponseDto response = this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "CURRENCY_EXCHANGE_RATE")
                        .queryParam("from_currency", cleanSymbol)
                        .queryParam("to_currency", "USD")
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(AlphaVantageResponseDto.class)
                .block();

        if (response != null && response.exchangeRate() != null && response.exchangeRate().exchangeRate() != null) {
            BigDecimal priceInUsd = response.exchangeRate().exchangeRate();

            // 2. Busca a cotação atualizada do Dólar em Reais (USD/BRL)
            BigDecimal usdToBrlRate = fetchUsdToBrlRate();

            // 3. Converte para BRL (Preço em USD * Taxa USD/BRL)
            return priceInUsd.multiply(usdToBrlRate);
        }

        if (response != null) {
            if (response.errorMessage() != null) {
                System.err.println("Erro Alpha Vantage: " + response.errorMessage());
            }
            if (response.note() != null) {
                System.err.println("Aviso Alpha Vantage: " + response.note());
            }
        }

        throw new RuntimeException("Não foi possível obter a cotação de cripto para o símbolo: " + symbol);
    }

    // Método auxiliar para buscar a cotação do Dólar em tempo real
    private BigDecimal fetchUsdToBrlRate() {
        try {
            WebClient currencyClient = WebClient.create("https://economia.awesomeapi.com.br");

            // Retorno JSON esperado: {"USDBRL": {"bid": "5.50"}}
            var response = currencyClient.get()
                    .uri("/json/last/USD-BRL")
                    .retrieve()
                    .bodyToMono(UsdRateResponseDto.class)
                    .block();

            if (response != null && response.usdBrl() != null) {
                return response.usdBrl().bid();
            }
        } catch (Exception e) {
            System.err.println("Falha ao buscar taxa de câmbio USD/BRL. Usando taxa fallback de 5.50: " + e.getMessage());
        }

        // Fallback em caso de instabilidade na API de câmbio
        return new BigDecimal("5.50");
    }

}
