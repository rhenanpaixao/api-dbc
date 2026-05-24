package com.dbc.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ano de fabricação do veículo")
public record YearDto(
        @JsonProperty("code")
        @Schema(description = "Código do ano/combustível na FIPE (ex: 2022-3)", example = "2022-3")
        String code,

        @JsonProperty("name")
        @Schema(description = "Descrição do ano e combustível", example = "2022 Diesel")
        String name
) {}
