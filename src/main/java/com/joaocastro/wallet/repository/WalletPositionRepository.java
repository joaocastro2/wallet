package com.joaocastro.wallet.repository;

import com.joaocastro.wallet.model.WalletPositionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletPositionRepository extends JpaRepository <WalletPositionModel, UUID> {

    Optional<WalletPositionModel> findByAssetSymbolIgnoreCase(String symbol);
    Optional<WalletPositionModel> findByAssetId(UUID assetId);

}
