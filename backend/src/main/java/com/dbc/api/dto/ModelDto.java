package com.dbc.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Modelo de veículo")
public record ModelDto(
        @JsonProperty("code")
        @Schema(description = "Código do modelo na FIPE", example = "5940")
        String code,

        @JsonProperty("name")
        @Schema(description = "Nome do modelo", example = "AMAROK High.CD 2.0 16V TDI 4x4 Dies. Aut")
        String name
) {}
