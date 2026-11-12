package com.joaocastro.wallet.model;

import com.joaocastro.wallet.model.enums.AssetType;
import com.joaocastro.wallet.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionModelTest {

    @Test
    void deveCalcularValorTotalEDataNaPersistencia() {
        AssetModel asset = new AssetModel();
        asset.setSymbol("PETR4");
        TransactionModel transaction = TransactionModel.builder()
                .asset(asset).type(TransactionType.BUY)
                .quantity(new BigDecimal("2.5")).unitPrice(new BigDecimal("40.00")).build();

        transaction.prePersist();

        assertThat(transaction.getTotalValue()).isEqualByComparingTo("100.00");
        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void devePreservarDataExistenteNaPersistencia() {
        AssetModel asset = new AssetModel();
        asset.setType(AssetType.STOCK);
        var date = java.time.LocalDateTime.of(2025, 1, 1, 10, 0);
        TransactionModel transaction = TransactionModel.builder()
                .asset(asset).type(TransactionType.SELL).quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.TEN).createdAt(date).build();

        transaction.prePersist();

        assertThat(transaction.getCreatedAt()).isEqualTo(date);
    }
}
