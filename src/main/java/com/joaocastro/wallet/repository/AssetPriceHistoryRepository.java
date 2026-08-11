package com.joaocastro.wallet.repository;

import com.joaocastro.wallet.model.AssetPriceHistoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetPriceHistoryRepository extends JpaRepository <AssetPriceHistoryModel, UUID> {

    List<AssetPriceHistoryModel> findByAssetSymbolIgnoreCaseOrderByRecordedAtDesc(String symbol);

}
