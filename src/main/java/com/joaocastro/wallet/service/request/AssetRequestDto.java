package com.joaocastro.wallet.service.request;

import jakarta.validation.constraints.NotBlank;

//DTO for manual creation/update (entry)
public record AssetRequestDto(

        @NotBlank(message = "The symbol is mandatory")
        String symbol,

        @NotBlank(message = "the name is mandatory")
        String name,

        @NotBlank(message = "the asset type is mandatory")
        String type

) {
}
