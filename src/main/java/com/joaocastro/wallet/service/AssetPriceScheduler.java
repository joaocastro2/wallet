package com.joaocastro.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetPriceScheduler {

    private final AssetService assetService;

    // Exemplo usando Cron: Roda de segunda a sexta, de hora em hora, das 09:00 às 18:00
    @Scheduled(fixedDelayString = "${wallet.scheduling.fixed-delay:60000}")
    public void schedulePriceUpdate() {
        log.info("Iniciando rotina automática de atualização de cotações dos ativos...");
        try {
            assetService.updateAllActiveAssetsPrices();
            log.info("Rotina automática de atualização concluída com sucesso!");
        } catch (Exception e) {
            log.error("Erro durante a rotina automática de atualização de preços: {}", e.getMessage(), e);
        }
    }
}
