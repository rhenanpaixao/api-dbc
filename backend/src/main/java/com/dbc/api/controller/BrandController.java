package com.dbc.api.controller;

import com.dbc.api.dto.BrandDto;
import com.dbc.api.dto.ModelDto;
import com.dbc.api.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Listagem de marcas e modelos FIPE")
public class BrandController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Lista todas as marcas disponíveis na tabela FIPE")
    public ResponseEntity<List<BrandDto>> getBrands(
            @Parameter(description = "Tipo de veículo", example = "cars")
            @RequestParam(defaultValue = "cars") String vehicleType
    ) {
        return ResponseEntity.ok(vehicleService.getBrands(vehicleType));
    }

    @GetMapping("/{brandId}/models")
    @Operation(summary = "Lista todos os modelos de uma marca")
    public ResponseEntity<List<ModelDto>> getModels(
            @Parameter(description = "ID da marca", example = "59")
            @PathVariable int brandId,
            @Parameter(description = "Tipo de veículo", example = "cars")
            @RequestParam(defaultValue = "cars") String vehicleType
    ) {
        return ResponseEntity.ok(vehicleService.getModels(vehicleType, brandId));
    }
}
