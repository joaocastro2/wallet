package com.joaocastro.wallet.service;

import com.joaocastro.wallet.event.TransactionCreatedEvent;
import com.joaocastro.wallet.kafka.producer.EventProducer;
import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.TransactionModel;
import com.joaocastro.wallet.model.WalletPositionModel;
import com.joaocastro.wallet.model.enums.AssetType;
import com.joaocastro.wallet.model.enums.TransactionType;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.repository.TransactionRepository;
import com.joaocastro.wallet.repository.WalletPositionRepository;
import com.joaocastro.wallet.service.request.TransactionRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String SYMBOL = "PETR4";
    private static final BigDecimal MARKET_PRICE = new BigDecimal("40.00");

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletPositionRepository walletPositionRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private EventProducer eventProducer;
    @InjectMocks private TransactionService service;

    @Test
    void deveRegistrarCompraEPublicarEvento_QuandoAtivoTemCotacaoValida() {
        AssetModel asset = asset();
        TransactionModel saved = transaction(TransactionType.BUY, asset, new BigDecimal("3"));
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.of(asset));
        given(transactionRepository.save(any(TransactionModel.class))).willReturn(saved);

        var result = service.buy(new TransactionRequestDto(SYMBOL, new BigDecimal("3")));

        assertThat(result.type()).isEqualTo(TransactionType.BUY);
        assertThat(result.assetSymbol()).isEqualTo(SYMBOL);
        ArgumentCaptor<TransactionCreatedEvent> event = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(eventProducer).sendTransactionCreatedEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(TransactionType.BUY);
        assertThat(event.getValue().unitPrice()).isEqualByComparingTo(MARKET_PRICE);
    }

    @Test
    void deveFalharCompra_QuandoAtivoNaoExistir() {
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.buy(new TransactionRequestDto(SYMBOL, BigDecimal.ONE)))
                .isInstanceOf(RuntimeException.class);
        then(transactionRepository).shouldHaveNoInteractions();
        then(eventProducer).shouldHaveNoInteractions();
    }

    @Test
    void deveFalharCompra_QuandoCotacaoForInvalida() {
        AssetModel asset = asset();
        asset.setCurrentPrice(BigDecimal.ZERO);
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.buy(new TransactionRequestDto(SYMBOL, BigDecimal.ONE)))
                .isInstanceOf(IllegalStateException.class);
        then(transactionRepository).shouldHaveNoInteractions();
    }

    @Test
    void deveRegistrarVenda_QuandoSaldoForSuficiente() {
        AssetModel asset = asset();
        WalletPositionModel position = WalletPositionModel.builder()
                .asset(asset).quantity(new BigDecimal("5")).averagePrice(new BigDecimal("30")).build();
        TransactionModel saved = transaction(TransactionType.SELL, asset, new BigDecimal("2"));
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.of(asset));
        given(walletPositionRepository.findByAssetId(asset.getId())).willReturn(Optional.of(position));
        given(transactionRepository.save(any(TransactionModel.class))).willReturn(saved);

        var result = service.sell(new TransactionRequestDto(SYMBOL, new BigDecimal("2")));

        assertThat(result.type()).isEqualTo(TransactionType.SELL);
        verify(eventProducer).sendTransactionCreatedEvent(any(TransactionCreatedEvent.class));
    }

    @Test
    void deveFalharVenda_QuandoSaldoForInsuficiente() {
        AssetModel asset = asset();
        WalletPositionModel position = WalletPositionModel.builder()
                .asset(asset).quantity(BigDecimal.ONE).averagePrice(new BigDecimal("30")).build();
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.of(asset));
        given(walletPositionRepository.findByAssetId(asset.getId())).willReturn(Optional.of(position));

        assertThatThrownBy(() -> service.sell(new TransactionRequestDto(SYMBOL, new BigDecimal("2"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insuficiente");
        then(transactionRepository).shouldHaveNoInteractions();
        then(eventProducer).shouldHaveNoInteractions();
    }

    @Test
    void deveFalharVenda_QuandoCarteiraNaoPossuirPosicao() {
        AssetModel asset = asset();
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.of(asset));
        given(walletPositionRepository.findByAssetId(asset.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sell(new TransactionRequestDto(SYMBOL, BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao possui");
    }

    @Test
    void deveRetornarExtratoOrdenado() {
        AssetModel asset = asset();
        given(transactionRepository.findAllByOrderByCreatedAtDesc())
                .willReturn(List.of(transaction(TransactionType.BUY, asset, BigDecimal.ONE)));

        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void deveFalharExtratoDoAtivo_QuandoAtivoNaoExistir() {
        given(assetRepository.existsBySymbolIgnoreCase(SYMBOL)).willReturn(false);

        assertThatThrownBy(() -> service.findByAssetSymbol(SYMBOL))
                .isInstanceOf(RuntimeException.class);
        then(transactionRepository).shouldHaveNoInteractions();
    }

    private static AssetModel asset() {
        AssetModel asset = new AssetModel();
        asset.setId(UUID.randomUUID());
        asset.setSymbol(SYMBOL);
        asset.setName("Petrobras");
        asset.setType(AssetType.STOCK);
        asset.setCurrentPrice(MARKET_PRICE);
        asset.setActive(true);
        return asset;
    }

    private static TransactionModel transaction(TransactionType type, AssetModel asset, BigDecimal quantity) {
        TransactionModel transaction = TransactionModel.builder()
                .id(UUID.randomUUID()).asset(asset).type(type).quantity(quantity)
                .unitPrice(MARKET_PRICE).build();
        transaction.prePersist();
        return transaction;
    }
}
