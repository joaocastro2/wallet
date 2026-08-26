package com.joaocastro.wallet.service;

import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.TransactionModel;
import com.joaocastro.wallet.model.WalletPositionModel;
import com.joaocastro.wallet.model.enums.TransactionType;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.repository.TransactionRepository;
import com.joaocastro.wallet.repository.WalletPositionRepository;
import com.joaocastro.wallet.service.request.TransactionRequestDto;
import com.joaocastro.wallet.service.response.TransactionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletPositionRepository walletPositionRepository;
    private final AssetRepository assetRepository;

    /**
     * Registra uma ordem de COMPRA, atualizando a quantidade acumulada e o Preço Médio.
     */
    @Transactional
    public TransactionResponseDto buy(TransactionRequestDto dto) {
        AssetModel asset = findAssetBySymbol(dto.symbol());

        // 1. Registra a Transação de Compra
        TransactionModel transaction = TransactionModel.builder()
                .asset(asset)
                .type(TransactionType.BUY)
                .quantity(dto.quantity())
                .unitPrice(dto.unitPrice())
                .build();

        TransactionModel savedTransaction = transactionRepository.save(transaction);

        // 2. Atualiza ou cria a Posição na Carteira com o novo Preço Médio
        WalletPositionModel position = walletPositionRepository.findByAssetId(asset.getId())
                .orElseGet(() -> WalletPositionModel.builder()
                        .asset(asset)
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .build());

        BigDecimal currentQty = position.getQuantity();
        BigDecimal currentAvgPrice = position.getAveragePrice();

        BigDecimal newQty = dto.quantity();
        BigDecimal buyPrice = dto.unitPrice();

        // Fórmula do Preço Médio Ponderado:
        // Novo PM = ((Qtd Atual * PM Atual) + (Qtd Nova * Preço Novo)) / (Qtd Total)
        BigDecimal totalCurrentCost = currentQty.multiply(currentAvgPrice);
        BigDecimal totalNewCost = newQty.multiply(buyPrice);
        BigDecimal totalQty = currentQty.add(newQty);

        BigDecimal newAveragePrice = totalCurrentCost.add(totalNewCost)
                .divide(totalQty, 4, RoundingMode.HALF_UP);

        position.setQuantity(totalQty);
        position.setAveragePrice(newAveragePrice);
        position.setUpdatedAt(LocalDateTime.now());

        walletPositionRepository.save(position);

        log.info("Compra efetuada: {}x {} a R$ {}. Novo PM: R$ {}", newQty, asset.getSymbol(), buyPrice, newAveragePrice);

        return TransactionResponseDto.fromEntity(savedTransaction);
    }

    /**
     * Registra uma ordem de VENDA após validar se há saldo suficiente na carteira.
     */
    @Transactional
    public TransactionResponseDto sell(TransactionRequestDto dto) {
        AssetModel asset = findAssetBySymbol(dto.symbol());

        // 1. Valida se o usuário possui posição aberta para este ativo
        WalletPositionModel position = walletPositionRepository.findByAssetId(asset.getId())
                .orElseThrow(() -> new IllegalArgumentException("Você não possui posição aberta do ativo: " + dto.symbol()));

        // 2. Validação de Saldo Insuficiente para Venda
        if (position.getQuantity().compareTo(dto.quantity()) < 0) {
            throw new IllegalArgumentException(
                    String.format("Saldo insuficiente para venda de %s. Saldo atual: %s, Tentativa de venda: %s",
                            asset.getSymbol(), position.getQuantity(), dto.quantity())
            );
        }

        // 3. Registra a Transação de Venda
        TransactionModel transaction = TransactionModel.builder()
                .asset(asset)
                .type(TransactionType.SELL)
                .quantity(dto.quantity())
                .unitPrice(dto.unitPrice())
                .build();

        TransactionModel savedTransaction = transactionRepository.save(transaction);

        // 4. Abate a quantidade vendida na Posição (O Preço Médio Permanece Inalterado)
        BigDecimal remainingQty = position.getQuantity().subtract(dto.quantity());
        position.setQuantity(remainingQty);
        position.setUpdatedAt(LocalDateTime.now());

        // Se zerou a quantidade mantida do ativo, zera o preço médio também
        if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
            position.setAveragePrice(BigDecimal.ZERO);
        }

        walletPositionRepository.save(position);

        log.info("Venda efetuada: {}x {} a R$ {}. Saldo restante: {}", dto.quantity(), asset.getSymbol(), dto.unitPrice(), remainingQty);

        return TransactionResponseDto.fromEntity(savedTransaction);
    }

    /**
     * Retorna o extrato completo de compras e vendas.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findAll() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TransactionResponseDto::fromEntity)
                .toList();
    }

    /**
     * Retorna o extrato de um ativo específico.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findByAssetSymbol(String symbol) {
        if (!assetRepository.existsBySymbolIgnoreCase(symbol)) {
            throw new RuntimeException("Ativo não encontrado com o símbolo: " + symbol);
        }

        return transactionRepository.findByAssetSymbolIgnoreCaseOrderByCreatedAtDesc(symbol)
                .stream()
                .map(TransactionResponseDto::fromEntity)
                .toList();
    }

    private AssetModel findAssetBySymbol(String symbol) {
        return assetRepository.findBySymbolIgnoreCase(symbol)
                .orElseThrow(() -> new RuntimeException("Ativo não encontrado com o símbolo: " + symbol));
    }

}
