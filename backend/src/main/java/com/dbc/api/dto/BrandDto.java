package com.dbc.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Marca de veículo")
public record BrandDto(
        @JsonProperty("code")
        @Schema(description = "Código da marca na FIPE", example = "59")
        String code,

        @JsonProperty("name")
        @Schema(description = "Nome da marca", example = "VW - VolksWagen")
        String name
) {}
