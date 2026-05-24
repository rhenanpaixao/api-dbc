package com.dbc.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dbc.api.client.FipeClient;
import com.dbc.api.dto.BrandDto;
import com.dbc.api.dto.FipeInfoDto;
import com.dbc.api.dto.ModelDto;
import com.dbc.api.dto.VehicleHistoryResponse;
import com.dbc.api.dto.YearDto;
import com.dbc.api.dto.YearlyPriceDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final FipeClient fipeClient;

    private static final Locale PT_BR = new Locale("pt", "BR");

    public List<BrandDto> getBrands(String vehicleType) {
        return fipeClient.getBrands(vehicleType);
    }

    public List<ModelDto> getModels(String vehicleType, int brandId) {
        return fipeClient.getModels(vehicleType, brandId);
    }

    /**
     * Busca os preços do veículo em cada ano de fabricação e calcula
     * a variação absoluta e percentual entre anos consecutivos.
     *
     * <p>A API FIPE retorna códigos de ano no formato "AAAA-C" onde C é o tipo
     * de combustível. Para evitar duplicatas, agrupamos por ano de fabricação
     * e mantemos apenas o primeiro combustível encontrado por ano.</p>
     */
    public VehicleHistoryResponse getVehiclePriceHistory(String vehicleType, int brandId, int modelId) {
        List<YearDto> allYears = fipeClient.getYears(vehicleType, brandId, modelId);

        // Agrupa por ano de fabricação mantendo o primeiro combustível por ano,
        // depois ordena do mais recente para o mais antigo.
        List<YearDto> uniqueYearsSorted = allYears.stream()
                .collect(Collectors.toMap(
                        year -> extractModelYear(year.code()),
                        Function.identity(),
                        (first, second) -> first   // mantém o primeiro combustível por ano
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, YearDto>comparingByKey().reversed())
                .map(Map.Entry::getValue)
                .toList();

        List<FipeInfoDto> fipeInfoList = uniqueYearsSorted.stream()
                .map(year -> fipeClient.getFipeInfo(vehicleType, brandId, modelId, year.code()))
                .toList();

        String brand = fipeInfoList.isEmpty() ? "" : fipeInfoList.get(0).brand();
        String model = fipeInfoList.isEmpty() ? "" : fipeInfoList.get(0).model();
        String fuel  = fipeInfoList.isEmpty() ? "" : fipeInfoList.get(0).fuel();

        return new VehicleHistoryResponse(brand, model, fuel, buildPriceHistory(fipeInfoList));
    }

    /**
     * Monta a lista de variações de preço ano a ano.
     *
     * <p>Para cada ano, calcula a diferença absoluta e o percentual em relação
     * ao ano imediatamente anterior (posição seguinte na lista ordenada).
     * O ano mais antigo não possui referência anterior, portanto os campos
     * de variação ficam nulos.</p>
     */
    private List<YearlyPriceDto> buildPriceHistory(List<FipeInfoDto> fipeInfoList) {
        List<YearlyPriceDto> result = new ArrayList<>();

        for (int i = 0; i < fipeInfoList.size(); i++) {
            FipeInfoDto current = fipeInfoList.get(i);
            BigDecimal currentPrice = parsePrice(current.price());

            boolean hasComparison = i + 1 < fipeInfoList.size();

            if (hasComparison) {
                FipeInfoDto previous = fipeInfoList.get(i + 1);
                BigDecimal previousPrice = parsePrice(previous.price());
                BigDecimal difference   = currentPrice.subtract(previousPrice);
                BigDecimal percentage   = calculatePercentage(difference, previousPrice);

                result.add(new YearlyPriceDto(
                        current.modelYear(),
                        currentPrice,
                        formatCurrency(currentPrice),
                        difference,
                        formatCurrency(difference),
                        percentage,
                        formatPercentage(percentage),
                        previous.modelYear()
                ));
            } else {
                result.add(new YearlyPriceDto(
                        current.modelYear(),
                        currentPrice,
                        formatCurrency(currentPrice),
                        null, null, null, null, null
                ));
            }
        }

        return result;
    }

    /**
     * Extrai o ano numérico de um código FIPE no formato "AAAA-C".
     * Exemplo: "2022-3" → 2022
     */
    private int extractModelYear(String yearCode) {
        return Integer.parseInt(yearCode.split("-")[0]);
    }

    /**
     * Converte o preço no formato FIPE ("R$ 25.000,00") para BigDecimal.
     */
    private BigDecimal parsePrice(String fipePrice) {
        String normalized = fipePrice
                .replace("R$", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();
        return new BigDecimal(normalized);
    }

    private BigDecimal calculatePercentage(BigDecimal difference, BigDecimal base) {
        return difference
                .divide(base, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    private String formatPercentage(BigDecimal value) {
        return NumberFormat.getPercentInstance(PT_BR).format(
                value.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );
    }
}
