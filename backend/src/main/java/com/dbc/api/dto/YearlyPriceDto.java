package com.dbc.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Preço do veículo em um ano de fabricação e sua variação em relação ao ano anterior")
public record YearlyPriceDto(

        @Schema(description = "Ano de fabricação", example = "2013")
        int year,

        @Schema(description = "Preço tabelado FIPE", example = "25000.00")
        BigDecimal price,

        @Schema(description = "Preço formatado", example = "R$ 25.000,00")
        String priceFormatted,

        @Schema(description = "Diferença monetária em relação ao ano anterior (null no ano mais antigo)", example = "2500.00")
        BigDecimal priceDifference,

        @Schema(description = "Diferença formatada", example = "R$ 2.500,00")
        String priceDifferenceFormatted,

        @Schema(description = "Percentual de variação em relação ao ano anterior (null no ano mais antigo)", example = "11.00")
        BigDecimal changePercentage,

        @Schema(description = "Percentual formatado", example = "11,00%")
        String changePercentageFormatted,

        @Schema(description = "Ano de referência para o cálculo da variação", example = "2011")
        Integer comparedToYear
) {}
