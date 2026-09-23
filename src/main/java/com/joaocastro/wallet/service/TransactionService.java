package com.joaocastro.wallet.service;

import com.joaocastro.wallet.event.TransactionCreatedEvent;
import com.joaocastro.wallet.kafka.producer.EventProducer;
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
    private final EventProducer eventProducer;

    /**
     * Registra uma ordem de COMPRA, atualizando a quantidade acumulada e o Preço Médio.
     */
    @Transactional
    public TransactionResponseDto buy(TransactionRequestDto dto) {
        AssetModel asset = findAssetBySymbol(dto.symbol());

        BigDecimal marketPrice = asset.getCurrentPrice();

        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("O ativo " + asset.getSymbol() + " não possui cotação válida de mercado.");
        }

        // 1. Registra a Transação de Compra
        TransactionModel transaction = TransactionModel.builder()
                .asset(asset)
                .type(TransactionType.BUY)
                .quantity(dto.quantity())
                .unitPrice(marketPrice)
                .build();

        TransactionModel savedTransaction = transactionRepository.save(transaction);

        // 2. Publica o evento no Kafka
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                savedTransaction.getId(),
                savedTransaction.getAsset().getSymbol(),
                savedTransaction.getType(),
                savedTransaction.getQuantity(),
                savedTransaction.getUnitPrice(),
                savedTransaction.getTotalValue(),
                savedTransaction.getCreatedAt()
        );

        eventProducer.sendTransactionCreatedEvent(event);

        log.info("Compra efetuada e evento publicado no Kafka: {}x {} a R$", dto.quantity(), asset.getSymbol(), marketPrice);

        return TransactionResponseDto.fromEntity(savedTransaction);
    }

    /**
     * Registra uma ordem de VENDA após validar se há saldo suficiente na carteira.
     */
    @Transactional
    public TransactionResponseDto sell(TransactionRequestDto dto) {
        AssetModel asset = findAssetBySymbol(dto.symbol());
        BigDecimal marketPrice = asset.getCurrentPrice();

        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("O ativo " + asset.getSymbol() + " não possui uma cotação de mercado válida para venda.");
        }

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
                .unitPrice(marketPrice)
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

        log.info("Venda efetuada: {}x {} a R$ {}. Saldo restante: {}", dto.quantity(), asset.getSymbol(), marketPrice, remainingQty);

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
