package com.joaocastro.wallet.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record UsdRateResponseDto(
        @JsonProperty("USDBRL")
        UsdData usdBrl
) {
    public record UsdData(
            @JsonProperty("bid")
            BigDecimal bid
    ) {}

}
