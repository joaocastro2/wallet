package com.joaocastro.wallet.service;

import com.joaocastro.wallet.client.BrapiClient;
import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.AssetPriceHistoryModel;
import com.joaocastro.wallet.repository.AssetPriceHistoryRepository;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.service.response.AssetPriceHistoryResponseDto;
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
    private final AssetPriceHistoryRepository assetPriceHistoryRepository;
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

        // 1. Busca o preço atualizado via Brapi Client
        BigDecimal updatedPrice = brapiClient.fetchCurrentPrice(asset.getSymbol());
        LocalDateTime now = LocalDateTime.now();

        // 2. Atualiza a entidade principal
        asset.setCurrentPrice(updatedPrice);
        asset.setLastPriceUpdate(now);

        // 3. Salva a foto do preço no Histórico
        savePriceHistory(asset, updatedPrice, now);

        return AssetResponseDto.fromEntity(asset);
    }

    @Transactional
    public void updateAllActiveAssetsPrices() {
        List<AssetModel> activeAssets = assetRepository.findByActiveTrue();

        for (AssetModel asset : activeAssets) {
            try {
                BigDecimal updatedPrice = brapiClient.fetchCurrentPrice(asset.getSymbol());
                LocalDateTime now = LocalDateTime.now();

                // Atualiza a cotação no ativo
                asset.setCurrentPrice(updatedPrice);
                asset.setLastPriceUpdate(now);

                // Grava o histórico
                savePriceHistory(asset, updatedPrice, now);

                // Pequeno delay preventivo entre chamadas HTTP
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("Erro ao atualizar {}: {}", asset.getSymbol(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AssetPriceHistoryResponseDto> findHistoryBySymbol(String symbol) {
        // Valida se o ativo existe antes de buscar o histórico
        if (!assetRepository.existsBySymbolIgnoreCase(symbol)) {
            throw new RuntimeException("Ativo não encontrado com o símbolo: " + symbol);
        }

        // Retorna a lista de históricos (se estiver vazio, retorna [] sem dar erro)
        return assetPriceHistoryRepository.findByAssetSymbolIgnoreCaseOrderByRecordedAtDesc(symbol)
                .stream()
                .map(AssetPriceHistoryResponseDto::fromEntity)
                .toList();
    }

    /**
     * Método auxiliar privado para encapsular a criação e salvamento do registro de histórico.
     */
    private void savePriceHistory(AssetModel asset, BigDecimal price, LocalDateTime recordedAt) {
        AssetPriceHistoryModel history = new AssetPriceHistoryModel();
        history.setAsset(asset);
        history.setPrice(price);
        history.setRecordedAt(recordedAt);

        assetPriceHistoryRepository.save(history);
    }
}