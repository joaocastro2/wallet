package com.joaocastro.wallet.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record AlphaVantageResponseDto(
        @JsonProperty("Global Quote")
        GlobalQuoteData globalQuote,

        @JsonProperty("Realtime Currency Exchange Rate")
        ExchangeRateData exchangeRate,

        @JsonProperty("Error Message")
        String errorMessage,

        @JsonProperty("Note")
        String note
) {
    public record GlobalQuoteData(
            @JsonProperty("01. symbol")
            String symbol,

            @JsonProperty("05. price")
            BigDecimal price
    ) {}

    public record ExchangeRateData(
            @JsonProperty("1. From_Currency Code")
            String currencyCode,

            @JsonProperty("5. Exchange Rate")
            BigDecimal exchangeRate
    ) {}
}