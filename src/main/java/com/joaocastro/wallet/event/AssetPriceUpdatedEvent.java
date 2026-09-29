package com.joaocastro.wallet.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetPriceUpdatedEvent(

        String symbol,
        BigDecimal newPrice,
        LocalDateTime updatedAt
) {}
