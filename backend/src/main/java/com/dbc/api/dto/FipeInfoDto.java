package com.dbc.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa a resposta da API FIPE para um veículo em um ano específico.
 * O campo {@code price} vem no formato brasileiro: "R$ 25.000,00".
 */
public record FipeInfoDto(
        @JsonProperty("brand")     String brand,
        @JsonProperty("codeFipe")  String codeFipe,
        @JsonProperty("fuel")      String fuel,
        @JsonProperty("model")     String model,
        @JsonProperty("modelYear") int modelYear,
        @JsonProperty("price")     String price,
        @JsonProperty("vehicleType") int vehicleType
) {}
