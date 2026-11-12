package com.joaocastro.wallet.service;

import com.joaocastro.wallet.event.TransactionCreatedEvent;
import com.joaocastro.wallet.event.UserCreatedEvent;
import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.WalletPositionModel;
import com.joaocastro.wallet.model.enums.AssetType;
import com.joaocastro.wallet.model.enums.TransactionType;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.repository.WalletPositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock private WalletPositionRepository walletPositionRepository;
    @Mock private AssetRepository assetRepository;
    @InjectMocks private PortfolioService service;

    @Test
    void deveCalcularResumoDaCarteiraComLucro() {
        AssetModel asset = asset(new BigDecimal("15"));
        WalletPositionModel position = WalletPositionModel.builder()
                .asset(asset).quantity(new BigDecimal("10")).averagePrice(new BigDecimal("10")).build();
        given(walletPositionRepository.findAll()).willReturn(List.of(position));

        var summary = service.getPortfolioSumary();

        assertThat(summary.totalInvested()).isEqualByComparingTo("100.00");
        assertThat(summary.totalCurrentValue()).isEqualByComparingTo("150.00");
        assertThat(summary.totalProfitOrLoss()).isEqualByComparingTo("50.00");
        assertThat(summary.totalReturnPercentage()).isEqualByComparingTo("50.00");
        assertThat(summary.items()).hasSize(1);
    }

    @Test
    void deveIgnorarPosicoesSemQuantidadePositiva() {
        WalletPositionModel position = WalletPositionModel.builder()
                .quantity(BigDecimal.ZERO).averagePrice(BigDecimal.TEN).build();
        given(walletPositionRepository.findAll()).willReturn(List.of(position));

        var summary = service.getPortfolioSumary();

        assertThat(summary.items()).isEmpty();
        assertThat(summary.totalInvested()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveProcessarCompraAtualizandoPrecoMedioPonderado() {
        AssetModel asset = asset(new BigDecimal("15"));
        WalletPositionModel position = WalletPositionModel.builder()
                .asset(asset).quantity(new BigDecimal("10")).averagePrice(new BigDecimal("10")).build();
        given(assetRepository.findBySymbolIgnoreCase("PETR4")).willReturn(Optional.of(asset));
        given(walletPositionRepository.findByAssetId(asset.getId())).willReturn(Optional.of(position));

        service.processTransactionEvent(new TransactionCreatedEvent(
                UUID.randomUUID(), "PETR4", TransactionType.BUY,
                new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("100"), LocalDateTime.now()));

        assertThat(position.getQuantity()).isEqualByComparingTo("15");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("13.3333");
        verify(walletPositionRepository).save(position);
    }

    @Test
    void deveZerarPrecoMedioAoVenderTodaPosicao() {
        AssetModel asset = asset(new BigDecimal("15"));
        WalletPositionModel position = WalletPositionModel.builder()
                .asset(asset).quantity(new BigDecimal("10")).averagePrice(new BigDecimal("10")).build();
        given(assetRepository.findBySymbolIgnoreCase("PETR4")).willReturn(Optional.of(asset));
        given(walletPositionRepository.findByAssetId(asset.getId())).willReturn(Optional.of(position));

        service.processTransactionEvent(new TransactionCreatedEvent(
                UUID.randomUUID(), "PETR4", TransactionType.SELL,
                new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("150"), LocalDateTime.now()));

        assertThat(position.getQuantity()).isEqualByComparingTo("0");
        assertThat(position.getAveragePrice()).isEqualByComparingTo("0");
    }

    @Test
    void deveCriarPosicaoAoProcessarCompraSemPosicaoExistente() {
        AssetModel asset = asset(new BigDecimal("15"));
        given(assetRepository.findBySymbolIgnoreCase("PETR4")).willReturn(Optional.of(asset));
        given(walletPositionRepository.findByAssetId(asset.getId())).willReturn(Optional.empty());

        service.processTransactionEvent(new TransactionCreatedEvent(
                UUID.randomUUID(), "PETR4", TransactionType.BUY,
                BigDecimal.ONE, new BigDecimal("15"), new BigDecimal("15"), LocalDateTime.now()));

        ArgumentCaptor<WalletPositionModel> captor = ArgumentCaptor.forClass(WalletPositionModel.class);
        verify(walletPositionRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("1");
        assertThat(captor.getValue().getAveragePrice()).isEqualByComparingTo("15");
    }

    @Test
    void deveFalharProcessamento_QuandoAtivoNaoExistir() {
        given(assetRepository.findBySymbolIgnoreCase("NOPE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.processTransactionEvent(new TransactionCreatedEvent(
                UUID.randomUUID(), "NOPE", TransactionType.BUY, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.TEN, LocalDateTime.now()))).isInstanceOf(IllegalArgumentException.class);
        then(walletPositionRepository).shouldHaveNoInteractions();
    }

    @Test
    void deveAceitarEventoDeUsuarioSemCriarPosicao() {
        service.processUserCreatedEvent(new UserCreatedEvent(UUID.randomUUID(), "Joao", "joao@email.com", LocalDateTime.now()));

        then(walletPositionRepository).shouldHaveNoInteractions();
        then(assetRepository).shouldHaveNoInteractions();
    }

    private static AssetModel asset(BigDecimal currentPrice) {
        AssetModel asset = new AssetModel();
        asset.setId(UUID.randomUUID());
        asset.setSymbol("PETR4");
        asset.setName("Petrobras");
        asset.setType(AssetType.STOCK);
        asset.setCurrentPrice(currentPrice);
        asset.setActive(true);
        return asset;
    }
}
