package com.joaocastro.wallet.controller;

import com.joaocastro.wallet.service.PortfolioService;
import com.joaocastro.wallet.service.response.WalletSumaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<WalletSumaryResponseDto> getPortfolioSumary(){
        WalletSumaryResponseDto sumary = portfolioService.getPortfolioSumary();
        return ResponseEntity.ok(sumary);
    }
}
