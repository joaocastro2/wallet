package com.joaocastro.wallet.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BrapiResponseDto {

    private List<BrapiResultDTO> results;

    @Data
    public static class BrapiResultDTO {
        private String symbol;
        private String shortName;

        @JsonProperty("regularMarketPrice")
        private BigDecimal regularMarketPrice;
    }

}
