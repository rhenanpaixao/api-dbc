package com.dbc.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Histórico de preços de um veículo ao longo dos anos de fabricação")
public record VehicleHistoryResponse(

        @Schema(description = "Nome da marca", example = "VW - VolksWagen")
        String brand,

        @Schema(description = "Nome do modelo", example = "AMAROK High.CD 2.0 16V TDI 4x4 Dies. Aut")
        String model,

        @Schema(description = "Tipo de combustível", example = "Diesel")
        String fuel,

        @Schema(description = "Lista de preços por ano, em ordem decrescente")
        List<YearlyPriceDto> priceHistory
) {}
