package com.joaocastro.wallet.service;

import com.joaocastro.wallet.client.AlphaVantageClient;
import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.enums.AssetType;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.service.response.AssetResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AlphaVantageClient alphaVantageClient;

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

        // Verifica se o tipo do ativo é CRYPTO (Ajuste caso o nome do seu Enum de cripto seja diferente)
        boolean isCrypto = asset.getType() == AssetType.CRYPTO;

        // Busca o preço atualizado via API externa passando a flag isCrypto
        BigDecimal updatedPrice = alphaVantageClient.fetchCurrentPrice(asset.getSymbol(), isCrypto);

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
                boolean isCrypto = asset.getType() == AssetType.CRYPTO;

                BigDecimal updatedPrice = alphaVantageClient.fetchCurrentPrice(asset.getSymbol(), isCrypto);
                asset.setCurrentPrice(updatedPrice);
                asset.setLastPriceUpdate(LocalDateTime.now());

                // Delay de 1.2s para evitar estourar o Rate Limit de requisições por minuto da Alpha Vantage
                Thread.sleep(12000);
            } catch (Exception e) {
                System.err.println("Erro ao atualizar " + asset.getSymbol() + ": " + e.getMessage());
            }
        }
    }
}