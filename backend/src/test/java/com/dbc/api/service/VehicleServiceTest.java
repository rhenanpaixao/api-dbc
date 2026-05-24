package com.dbc.api.service;

import com.dbc.api.client.FipeClient;
import com.dbc.api.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private FipeClient fipeClient;

    @InjectMocks
    private VehicleService vehicleService;

    private static final String CARS = "cars";
    private static final int BRAND_ID = 59;
    private static final int MODEL_ID = 5940;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FipeInfoDto fipeInfo(int year, String price) {
        return new FipeInfoDto("VW - VolksWagen", "001234-5", "Gasolina", "Gol", year, price, 1);
    }

    private void stubYears(YearDto... years) {
        when(fipeClient.getYears(CARS, BRAND_ID, MODEL_ID)).thenReturn(List.of(years));
    }

    private void stubFipeInfo(String yearCode, int year, String price) {
        when(fipeClient.getFipeInfo(CARS, BRAND_ID, MODEL_ID, yearCode)).thenReturn(fipeInfo(year, price));
    }

    // -------------------------------------------------------------------------
    // Cenário principal: histórico com variação entre anos
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnPriceHistoryInDescendingOrder() {
        // Arrange
        stubYears(
                new YearDto("2013-1", "2013 Gasolina"),
                new YearDto("2011-1", "2011 Gasolina"),
                new YearDto("2009-1", "2009 Gasolina")
        );
        stubFipeInfo("2013-1", 2013, "R$ 25.000,00");
        stubFipeInfo("2011-1", 2011, "R$ 22.500,00");
        stubFipeInfo("2009-1", 2009, "R$ 18.225,00");

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);

        // Assert
        assertThat(response.brand()).isEqualTo("VW - VolksWagen");
        assertThat(response.model()).isEqualTo("Gol");
        assertThat(response.priceHistory()).hasSize(3);

        List<Integer> years = response.priceHistory().stream().map(YearlyPriceDto::year).toList();
        assertThat(years).containsExactly(2013, 2011, 2009);
    }

    @Test
    void shouldCalculateVariationCorrectlyBetweenConsecutiveYears() {
        // Arrange
        stubYears(
                new YearDto("2013-1", "2013 Gasolina"),
                new YearDto("2011-1", "2011 Gasolina")
        );
        stubFipeInfo("2013-1", 2013, "R$ 25.000,00");
        stubFipeInfo("2011-1", 2011, "R$ 22.500,00");

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);
        YearlyPriceDto most_recent = response.priceHistory().get(0);

        // Assert
        // Diferença: 25.000 - 22.500 = 2.500
        assertThat(most_recent.priceDifference()).isEqualByComparingTo(new BigDecimal("2500.00"));
        // Percentual: (2500 / 22500) * 100 = 11.11%
        assertThat(most_recent.changePercentage()).isEqualByComparingTo(new BigDecimal("11.11"));
        assertThat(most_recent.comparedToYear()).isEqualTo(2011);
    }

    @Test
    void shouldReturnNullVariationFieldsForOldestYear() {
        // Arrange
        stubYears(
                new YearDto("2022-1", "2022 Gasolina"),
                new YearDto("2021-1", "2021 Gasolina")
        );
        stubFipeInfo("2022-1", 2022, "R$ 100.000,00");
        stubFipeInfo("2021-1", 2021, "R$ 90.000,00");

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);
        YearlyPriceDto oldest = response.priceHistory().get(1);

        // Assert: o ano mais antigo não tem referência anterior
        assertThat(oldest.year()).isEqualTo(2021);
        assertThat(oldest.priceDifference()).isNull();
        assertThat(oldest.priceDifferenceFormatted()).isNull();
        assertThat(oldest.changePercentage()).isNull();
        assertThat(oldest.changePercentageFormatted()).isNull();
        assertThat(oldest.comparedToYear()).isNull();
    }

    @Test
    void shouldReturnSingleEntryWithNoVariationWhenOnlyOneYear() {
        // Arrange
        stubYears(new YearDto("2020-1", "2020 Gasolina"));
        stubFipeInfo("2020-1", 2020, "R$ 50.000,00");

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);

        // Assert
        assertThat(response.priceHistory()).hasSize(1);
        YearlyPriceDto only = response.priceHistory().get(0);
        assertThat(only.price()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(only.changePercentage()).isNull();
    }

    // -------------------------------------------------------------------------
    // Deduplicação: mesmo ano, múltiplos combustíveis
    // -------------------------------------------------------------------------

    @Test
    void shouldDeduplicateYearsKeepingFirstFuelType() {
        // Arrange: ano 2022 aparece 3x com combustíveis diferentes
        stubYears(
                new YearDto("2022-1", "2022 Gasolina"),
                new YearDto("2022-2", "2022 Álcool"),
                new YearDto("2022-3", "2022 Diesel"),
                new YearDto("2021-1", "2021 Gasolina")
        );
        stubFipeInfo("2022-1", 2022, "R$ 100.000,00");
        stubFipeInfo("2021-1", 2021, "R$ 90.000,00");

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);

        // Assert: apenas 2 entradas, não 4
        assertThat(response.priceHistory()).hasSize(2);
        // Os outros combustíveis do mesmo ano não devem ser consultados
        verify(fipeClient, never()).getFipeInfo(CARS, BRAND_ID, MODEL_ID, "2022-2");
        verify(fipeClient, never()).getFipeInfo(CARS, BRAND_ID, MODEL_ID, "2022-3");
    }

    // -------------------------------------------------------------------------
    // Ordenação garantida independente da ordem recebida
    // -------------------------------------------------------------------------

    @Test
    void shouldSortYearsDescendingRegardlessOfApiReturnOrder() {
        // Arrange: API retorna em ordem aleatória
        stubYears(
                new YearDto("2019-1", "2019 Gasolina"),
                new YearDto("2021-1", "2021 Gasolina"),
                new YearDto("2020-1", "2020 Gasolina")
        );
        when(fipeClient.getFipeInfo(eq(CARS), eq(BRAND_ID), eq(MODEL_ID), anyString()))
                .thenAnswer(inv -> {
                    String yearCode = inv.getArgument(3);
                    int year = Integer.parseInt(yearCode.split("-")[0]);
                    return fipeInfo(year, "R$ 10.000,00");
                });

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);

        // Assert
        List<Integer> years = response.priceHistory().stream().map(YearlyPriceDto::year).toList();
        assertThat(years).containsExactly(2021, 2020, 2019);
    }

    // -------------------------------------------------------------------------
    // Formatação monetária pt-BR
    // -------------------------------------------------------------------------

    @Test
    void shouldFormatPriceInPtBrCurrency() {
        // Arrange
        stubYears(new YearDto("2022-1", "2022 Gasolina"));
        stubFipeInfo("2022-1", 2022, "R$ 25.000,00");

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);
        String formatted = response.priceHistory().get(0).priceFormatted();

        // Assert: deve conter o padrão brasileiro com ponto como separador de milhar
        assertThat(formatted).contains("25.000");
        assertThat(formatted).contains("R$");
    }

    // -------------------------------------------------------------------------
    // Lista vazia
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnEmptyHistoryWhenNoYearsAvailable() {
        // Arrange
        when(fipeClient.getYears(CARS, BRAND_ID, MODEL_ID)).thenReturn(List.of());

        // Act
        VehicleHistoryResponse response = vehicleService.getVehiclePriceHistory(CARS, BRAND_ID, MODEL_ID);

        // Assert
        assertThat(response.priceHistory()).isEmpty();
        assertThat(response.brand()).isEmpty();
        assertThat(response.model()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Delegações para getBrands e getModels
    // -------------------------------------------------------------------------

    @Test
    void shouldDelegateBrandsToClient() {
        List<BrandDto> brands = List.of(new BrandDto("59", "VW - VolksWagen"));
        when(fipeClient.getBrands(CARS)).thenReturn(brands);

        assertThat(vehicleService.getBrands(CARS)).isEqualTo(brands);
        verify(fipeClient).getBrands(CARS);
    }

    @Test
    void shouldDelegateModelsToClient() {
        List<ModelDto> models = List.of(new ModelDto("5940", "AMAROK"));
        when(fipeClient.getModels(CARS, BRAND_ID)).thenReturn(models);

        assertThat(vehicleService.getModels(CARS, BRAND_ID)).isEqualTo(models);
        verify(fipeClient).getModels(CARS, BRAND_ID);
    }
}
