package com.joaocastro.wallet.service.response;

import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO for response (output)
public record AssetResponseDto(

        String symbol,
        String name,
        AssetType type,// ex: STOCK, REIT, CRYPTO
        Boolean active,
        BigDecimal currentPrice,
        LocalDateTime latsPriceUpdate

) {
    // Static mapping of Model -> DTO
    public static AssetResponseDto fromEntity(AssetModel asset) {
        return new AssetResponseDto(
                asset.getSymbol(),
                asset.getName(),
                asset.getType(),
                asset.isActive(),
                asset.getCurrentPrice(),
                asset.getLastPriceUpdate()
        );
    }
}
