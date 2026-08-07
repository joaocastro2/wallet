package com.joaocastro.wallet.repository;

import com.joaocastro.wallet.model.AssetModel;
import com.joaocastro.wallet.model.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository <AssetModel, UUID> {

    // Search for an asset by its symbol (e.g., "PETR4", "AAPL"), ignoring case.
    Optional<AssetModel> findBySymbolIgnoreCase(String symbol);

    // Checks if an asset with a specific symbol already exists.
    boolean existsBySymbolIgnoreCase(String symbol);

    // It only searches for assets that are active in the system.
    List<AssetModel> findByActiveTrue();

    // Search for assets by type (e.g., show only STOCKS or REITs)
    List<AssetModel> findByType(AssetType type);

    // Search for assets by type that are currently active.
    List<AssetModel> findByTypeAndActiveTrue(AssetType type);

}
