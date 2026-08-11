package com.joaocastro.wallet.service;

import com.joaocastro.wallet.client.BrapiClient;
import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.enums.AssetType;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.service.response.AssetResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final BrapiClient brapiClient;

    @Transactional(readOnly = true)
    public List<AssetResponseDto> findAllActive() {
        return assetRepository.findByActiveTrue()
                .stream()
                .map(AssetResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetResponseDto findBySymbol(String symbol) {
        AssetModel asset = assetRepository.findBySymbolIgnoreCase(symbol)
                .orElseThrow(() -> new RuntimeException("Ativo não encontrado com o símbolo: " + symbol));
        return AssetResponseDto.fromEntity(asset);
    }

    @Transactional
    public AssetResponseDto updatePriceFromExternalApi(String symbol) {
        AssetModel asset = assetRepository.findBySymbolIgnoreCase(symbol)
                .orElseThrow(() -> new RuntimeException("Ativo não encontrado com o símbolo: " + symbol));

        // Busca o preço atualizado via Brapi Client
        BigDecimal updatedPrice = brapiClient.fetchCurrentPrice(asset.getSymbol());

        // Atualiza a entidade no banco de dados
        asset.setCurrentPrice(updatedPrice);
        asset.setLastPriceUpdate(LocalDateTime.now());

        return AssetResponseDto.fromEntity(asset);
    }

    @Transactional
    public void updateAllActiveAssetsPrices() {
        List<AssetModel> activeAssets = assetRepository.findByActiveTrue();
        for (AssetModel asset : activeAssets) {
            try {
                BigDecimal updatedPrice = brapiClient.fetchCurrentPrice(asset.getSymbol());
                asset.setCurrentPrice(updatedPrice);
                asset.setLastPriceUpdate(LocalDateTime.now());

                // Pequeno delay de 200ms entre as chamadas apenas para boa prática
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("Erro ao atualizar {}: {}", asset.getSymbol(), e.getMessage());
            }
        }
    }

}