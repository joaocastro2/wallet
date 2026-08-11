package com.joaocastro.wallet.service.response;

import com.joaocastro.wallet.model.AssetPriceHistoryModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetPriceHistoryResponseDto(

        BigDecimal price,
        LocalDateTime recordedAt
) {

    public static AssetPriceHistoryResponseDto fromEntity(AssetPriceHistoryModel entity) {
        return new AssetPriceHistoryResponseDto(
                entity.getPrice(),
                entity.getRecordedAt()
        );
    }
}
