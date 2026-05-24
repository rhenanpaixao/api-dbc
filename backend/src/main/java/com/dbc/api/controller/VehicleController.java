package com.dbc.api.controller;

import com.dbc.api.dto.VehicleHistoryResponse;
import com.dbc.api.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(name = "Vehicles", description = "Histórico de preços de veículos FIPE")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping("/{brandId}/{modelId}")
    @Operation(
            summary = "Consulta histórico de preços de um veículo",
            description = "Retorna o preço FIPE de cada ano de fabricação do veículo e a variação absoluta/percentual em relação ao ano anterior."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "Marca ou modelo não encontrado na FIPE",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))),
            @ApiResponse(responseCode = "502", description = "Erro ao comunicar com a API FIPE",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    })
    public ResponseEntity<VehicleHistoryResponse> getVehiclePriceHistory(
            @Parameter(description = "ID da marca FIPE", example = "59", required = true)
            @PathVariable @Min(1) int brandId,

            @Parameter(description = "ID do modelo FIPE", example = "5940", required = true)
            @PathVariable @Min(1) int modelId,

            @Parameter(description = "Tipo de veículo: cars | motorcycles | trucks", example = "cars")
            @RequestParam(defaultValue = "cars") String vehicleType
    ) {
        return ResponseEntity.ok(vehicleService.getVehiclePriceHistory(vehicleType, brandId, modelId));
    }
}
