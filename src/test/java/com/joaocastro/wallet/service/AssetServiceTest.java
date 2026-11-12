package com.joaocastro.wallet.service;

import com.joaocastro.wallet.client.BrapiClient;
import com.joaocastro.wallet.event.AssetPriceUpdatedEvent;
import com.joaocastro.wallet.kafka.producer.EventProducer;
import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.AssetPriceHistoryModel;
import com.joaocastro.wallet.model.enums.AssetType;
import com.joaocastro.wallet.repository.AssetPriceHistoryRepository;
import com.joaocastro.wallet.repository.AssetRepository;
import com.joaocastro.wallet.service.response.AssetResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    private static final String SYMBOL = "PETR4";
    private static final BigDecimal PRICE = new BigDecimal("37.25");

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetPriceHistoryRepository assetPriceHistoryRepository;
    @Mock
    private BrapiClient brapiClient;
    @Mock
    private EventProducer eventProducer;
    @InjectMocks
    private AssetService service;

    @Nested
    @DisplayName("Consultas de ativos")
    class Consultas {
        @Test
        void deveRetornarAtivosAtivos_QuandoExistirem() {
            AssetModel asset = asset(SYMBOL, true);
            given(assetRepository.findByActiveTrue()).willReturn(List.of(asset));

            List<AssetResponseDto> result = service.findAllActive();

            assertThat(result).singleElement().extracting(AssetResponseDto::symbol).isEqualTo(SYMBOL);
        }

        @Test
        void deveLancarExcecao_QuandoSimboloNaoForEncontrado() {
            given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findBySymbol(SYMBOL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(SYMBOL);
        }

        @Test
        void deveRetornarHistorico_QuandoAtivoExistir() {
            AssetModel asset = asset(SYMBOL, true);
            AssetPriceHistoryModel history = new AssetPriceHistoryModel();
            history.setAsset(asset);
            history.setPrice(PRICE);
            given(assetRepository.existsBySymbolIgnoreCase(SYMBOL)).willReturn(true);
            given(assetPriceHistoryRepository.findByAssetSymbolIgnoreCaseOrderByRecordedAtDesc(SYMBOL))
                    .willReturn(List.of(history));

            assertThat(service.findHistoryBySymbol(SYMBOL)).singleElement()
                    .extracting(item -> item.price()).isEqualTo(PRICE);
        }

        @Test
        void deveLancarExcecaoAoBuscarHistorico_QuandoAtivoNaoExistir() {
            given(assetRepository.existsBySymbolIgnoreCase(SYMBOL)).willReturn(false);

            assertThatThrownBy(() -> service.findHistoryBySymbol(SYMBOL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(SYMBOL);
            then(assetPriceHistoryRepository).shouldHaveNoInteractions();
        }
    }

    @Test
    void deveAtualizarPrecoSalvarHistoricoEPublicarEvento_QuandoApiResponder() {
        AssetModel asset = asset(SYMBOL, true);
        given(assetRepository.findBySymbolIgnoreCase(SYMBOL)).willReturn(Optional.of(asset));
        given(brapiClient.fetchCurrentPrice(SYMBOL)).willReturn(PRICE);

        AssetResponseDto result = service.updatePriceFromExternalApi(SYMBOL);

        assertThat(result.currentPrice()).isEqualByComparingTo(PRICE);
        assertThat(asset.getLastPriceUpdate()).isNotNull();
        ArgumentCaptor<AssetPriceHistoryModel> history = ArgumentCaptor.forClass(AssetPriceHistoryModel.class);
        verify(assetPriceHistoryRepository).save(history.capture());
        assertThat(history.getValue().getAsset()).isSameAs(asset);
        assertThat(history.getValue().getPrice()).isEqualByComparingTo(PRICE);
        ArgumentCaptor<AssetPriceUpdatedEvent> event = ArgumentCaptor.forClass(AssetPriceUpdatedEvent.class);
        verify(eventProducer).sendAssetPriceUpdatedEvent(event.capture());
        assertThat(event.getValue().symbol()).isEqualTo(SYMBOL);
        assertThat(event.getValue().newPrice()).isEqualByComparingTo(PRICE);
    }

    @Test
    void deveIgnorarFalhaDeUmAtivoEContinuarAtualizandoOsDemais() {
        AssetModel failed = asset("FAIL3", true);
        AssetModel successful = asset("OK4", true);
        given(assetRepository.findByActiveTrue()).willReturn(List.of(failed, successful));
        given(brapiClient.fetchCurrentPrice("FAIL3")).willThrow(new RuntimeException("api indisponivel"));
        given(brapiClient.fetchCurrentPrice("OK4")).willReturn(PRICE);

        service.updateAllActiveAssetsPrices();

        assertThat(successful.getCurrentPrice()).isEqualByComparingTo(PRICE);
        verify(assetPriceHistoryRepository).save(argThat(history -> history.getAsset() == successful));
    }

    private static AssetModel asset(String symbol, boolean active) {
        AssetModel asset = new AssetModel();
        asset.setSymbol(symbol);
        asset.setName("Asset " + symbol);
        asset.setType(AssetType.STOCK);
        asset.setActive(active);
        return asset;
    }
}
