package com.joaocastro.wallet.service.response;

import com.joaocastro.wallet.model.TransactionModel;
import com.joaocastro.wallet.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDto(

        UUID id,
        String assetSymbol,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalValue,
        LocalDateTime createdAt

) {
    public static TransactionResponseDto fromEntity(TransactionModel entity) {
        return new TransactionResponseDto(
                entity.getId(),
                entity.getAsset().getSymbol(),
                entity.getType(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getTotalValue(),
                entity.getCreatedAt()
        );
    }
}
