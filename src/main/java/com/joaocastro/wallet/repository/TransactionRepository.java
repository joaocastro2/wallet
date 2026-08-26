package com.joaocastro.wallet.repository;

import com.joaocastro.wallet.model.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionModel, UUID> {

    List<TransactionModel> findAllByOrderByCreatedAtDesc();
    List<TransactionModel> findByAssetSymbolIgnoreCaseOrderByCreatedAtDesc(String symbol);

}
