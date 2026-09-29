package com.joaocastro.wallet.service.response;

import java.math.BigDecimal;

public record WalletItemResponseDto(

        String symbol,
        String assetName,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalInvested,      // Quantidade * Preço Médio
        BigDecimal currentTotalValue,  // Quantidade * Preço Atual
        BigDecimal profitOrLoss,       // Valor Atual - Valor Investido
        BigDecimal returnPercentage

) {
}
