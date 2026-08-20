package com.joaocastro.wallet.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BuyAssetRequestDto(

        @NotBlank(message = "O símbolo do ativo é obrigatório")
        String symbol,

        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        BigDecimal quantity,

        @NotNull(message = "O preço unitário é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal unitPrice

){}
