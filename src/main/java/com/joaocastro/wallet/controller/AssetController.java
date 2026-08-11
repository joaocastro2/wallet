package com.joaocastro.wallet.controller;

import com.joaocastro.wallet.service.AssetService;
import com.joaocastro.wallet.service.response.AssetPriceHistoryResponseDto;
import com.joaocastro.wallet.service.response.AssetResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    /**
     * Lists all assets that are marked as assets. (active = true).
     * Endpoint: GET /api/v1/assets
     */
    @GetMapping
    public ResponseEntity<List<AssetResponseDto>> getAllActiveAssets() {
        List<AssetResponseDto> assets = assetService.findAllActive();
        return ResponseEntity.ok(assets);
    }

    /**
     * Search for information about a specific asset using its symbol.
     * Endpoint: GET /api/v1/assets/{symbol} (ex: GET /api/v1/assets/PETR4)
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<AssetResponseDto> getAssetBySymbol(@PathVariable String symbol) {
        AssetResponseDto asset = assetService.findBySymbol(symbol);
        return ResponseEntity.ok(asset);
    }

    @PatchMapping("/{symbol}/update-price")
    public ResponseEntity<AssetResponseDto> updateAssetPrice(@PathVariable String symbol) {
        AssetResponseDto updatedAsset = assetService.updatePriceFromExternalApi(symbol);
        return ResponseEntity.ok(updatedAsset);
    }

    @PatchMapping("/update-all-prices")
    public ResponseEntity<Void> updateAllPrices() {
        assetService.updateAllActiveAssetsPrices();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<AssetPriceHistoryResponseDto>> findHistoryBySymbol(@PathVariable String symbol) {
        List<AssetPriceHistoryResponseDto> history = assetService.findHistoryBySymbol(symbol);
        return ResponseEntity.ok(history);
    }
}
