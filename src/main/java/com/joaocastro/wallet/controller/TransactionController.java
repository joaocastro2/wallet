package com.joaocastro.wallet.controller;

import com.joaocastro.wallet.service.TransactionService;
import com.joaocastro.wallet.service.request.TransactionRequestDto;
import com.joaocastro.wallet.service.response.TransactionResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Registra uma ordem de COMPRA de ativo.
     * HTTP 201 Created
     */
    @PostMapping("/buy")
    public ResponseEntity<TransactionResponseDto> buy(@Valid @RequestBody TransactionRequestDto request) {
        TransactionResponseDto response = transactionService.buy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Registra uma ordem de VENDA de ativo.
     * HTTP 201 Created
     */
    @PostMapping("/sell")
    public ResponseEntity<TransactionResponseDto> sell(@Valid @RequestBody TransactionRequestDto request) {
        TransactionResponseDto response = transactionService.sell(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retorna o extrato geral de todas as operações (compras e vendas).
     * HTTP 200 OK
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponseDto>> findAll() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    /**
     * Retorna o extrato de operações filtrado por um ativo específico.
     * HTTP 200 OK
     */
    @GetMapping("/asset/{symbol}")
    public ResponseEntity<List<TransactionResponseDto>> findByAssetSymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(transactionService.findByAssetSymbol(symbol));
    }
}