package com.joaocastro.wallet.event;

import com.joaocastro.wallet.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionCreatedEvent(

        UUID transactionId,
        String assetSymbol,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalValue,
        LocalDateTime createdAt
) {
}
