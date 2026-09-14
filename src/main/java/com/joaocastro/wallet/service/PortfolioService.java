package com.joaocastro.wallet.service;

import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.WalletPositionModel;
import com.joaocastro.wallet.repository.WalletPositionRepository;
import com.joaocastro.wallet.service.response.WalletItemResponseDto;
import com.joaocastro.wallet.service.response.WalletSumaryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final WalletPositionRepository walletPositionRepository;

    public WalletSumaryResponseDto getPortfolioSumary(){
        List<WalletPositionModel> positions = walletPositionRepository.findAll();

        BigDecimal grandTotalInvested = BigDecimal.ZERO;
        BigDecimal grandTotalCurrentValue = BigDecimal.ZERO;
        List<WalletItemResponseDto> items = new ArrayList<>();

        for (WalletPositionModel position : positions){
            if (position.getQuantity() == null || position.getQuantity().compareTo(BigDecimal.ZERO) <= 0){
                continue;
            }

            AssetModel asset = position.getAsset();
            BigDecimal qty = position.getQuantity();
            BigDecimal avgPrice = position.getAveragePrice();
            BigDecimal currentPrice = asset.getCurrentPrice() != null ? asset.getCurrentPrice() : avgPrice;

            BigDecimal totalInvested = qty.multiply(avgPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentTotalValue = qty.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profitOrLoss = currentTotalValue.subtract(totalInvested).setScale(2, RoundingMode.HALF_UP);

            BigDecimal returnPercentage = BigDecimal.ZERO;
            if (avgPrice.compareTo(BigDecimal.ZERO) > 0){
                returnPercentage = currentPrice.subtract(avgPrice)
                        .divide(avgPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            items.add(new WalletItemResponseDto(
                    asset.getSymbol(),
                    asset.getName(),
                    qty,
                    avgPrice.setScale(2, RoundingMode.HALF_UP),
                    currentPrice.setScale(2, RoundingMode.HALF_UP),
                    totalInvested,
                    currentTotalValue,
                    profitOrLoss,
                    returnPercentage
            ));

            grandTotalInvested = grandTotalInvested.add(totalInvested);
            grandTotalCurrentValue = grandTotalCurrentValue.add(currentTotalValue);

        }

        BigDecimal grandTotalProfitOrLoss = grandTotalCurrentValue.subtract(grandTotalInvested)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grandTotalReturnPercentage = BigDecimal.ZERO;
        if (grandTotalInvested.compareTo(BigDecimal.ZERO) > 0){
            grandTotalReturnPercentage = grandTotalProfitOrLoss
                    .divide(grandTotalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new WalletSumaryResponseDto(
                grandTotalInvested.setScale(2, RoundingMode.HALF_UP),
                grandTotalCurrentValue.setScale(2, RoundingMode.HALF_UP),
                grandTotalProfitOrLoss,
                grandTotalReturnPercentage,
                items
        );
    }

}
