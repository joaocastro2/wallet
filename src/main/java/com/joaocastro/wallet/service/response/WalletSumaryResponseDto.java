package com.joaocastro.wallet.service.response;

import java.math.BigDecimal;
import java.util.List;

public record WalletSumaryResponseDto(

        BigDecimal totalInvested,          // Total aplicado na carteira (R$)
        BigDecimal totalCurrentValue,      // Patrimônio Total Atual (R$)
        BigDecimal totalProfitOrLoss,      // Lucro ou Prejuízo Consolidado (R$)
        BigDecimal totalReturnPercentage,  // Rentabilidade Geral (%)
        List<WalletItemResponseDto> items   // Posições individuais mantidas

){}
