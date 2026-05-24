package com.dbc.api.controller;

import com.dbc.api.dto.VehicleHistoryResponse;
import com.dbc.api.dto.YearlyPriceDto;
import com.dbc.api.exception.FipeApiException;
import com.dbc.api.exception.ResourceNotFoundException;
import com.dbc.api.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private VehicleHistoryResponse sampleHistory() {
        YearlyPriceDto recent = new YearlyPriceDto(
                2022, new BigDecimal("100000.00"), "R$ 100.000,00",
                new BigDecimal("10000.00"), "R$ 10.000,00",
                new BigDecimal("11.11"), "11,11%", 2021
        );
        YearlyPriceDto oldest = new YearlyPriceDto(
                2021, new BigDecimal("90000.00"), "R$ 90.000,00",
                null, null, null, null, null
        );
        return new VehicleHistoryResponse("VW - VolksWagen", "Gol", "Gasolina", List.of(recent, oldest));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/vehicles/{brandId}/{modelId} — sucesso
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn200WithFullHistoryForValidIds() throws Exception {
        when(vehicleService.getVehiclePriceHistory("cars", 59, 5940))
                .thenReturn(sampleHistory());

        mockMvc.perform(get("/api/v1/vehicles/59/5940").param("vehicleType", "cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("VW - VolksWagen"))
                .andExpect(jsonPath("$.model").value("Gol"))
                .andExpect(jsonPath("$.fuel").value("Gasolina"))
                .andExpect(jsonPath("$.priceHistory", hasSize(2)))
                .andExpect(jsonPath("$.priceHistory[0].year").value(2022))
                .andExpect(jsonPath("$.priceHistory[0].priceFormatted").value("R$ 100.000,00"))
                .andExpect(jsonPath("$.priceHistory[0].changePercentage").value(11.11))
                .andExpect(jsonPath("$.priceHistory[0].comparedToYear").value(2021));
    }

    @Test
    void shouldUseDefaultVehicleTypeCarWhenParamIsOmitted() throws Exception {
        when(vehicleService.getVehiclePriceHistory("cars", 59, 5940))
                .thenReturn(sampleHistory());

        mockMvc.perform(get("/api/v1/vehicles/59/5940"))
                .andExpect(status().isOk());

        verify(vehicleService).getVehiclePriceHistory("cars", 59, 5940);
    }

    @Test
    void shouldOmitNullVariationFieldsFromJsonForOldestYear() throws Exception {
        when(vehicleService.getVehiclePriceHistory("cars", 59, 5940))
                .thenReturn(sampleHistory());

        mockMvc.perform(get("/api/v1/vehicles/59/5940"))
                .andExpect(status().isOk())
                // campos nulos não devem aparecer no JSON por causa do @JsonInclude(NON_NULL)
                .andExpect(jsonPath("$.priceHistory[1].priceDifference").doesNotExist())
                .andExpect(jsonPath("$.priceHistory[1].changePercentage").doesNotExist())
                .andExpect(jsonPath("$.priceHistory[1].comparedToYear").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // Validação de parâmetros — 400
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn400WhenBrandIdIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/0/5940"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Parâmetro inválido")));

        verify(vehicleService, never()).getVehiclePriceHistory(any(), anyInt(), anyInt());
    }

    @Test
    void shouldReturn400WhenModelIdIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/59/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(vehicleService, never()).getVehiclePriceHistory(any(), anyInt(), anyInt());
    }

    // -------------------------------------------------------------------------
    // Propagação de exceções do serviço
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn404WhenResourceNotFoundExceptionIsThrown() throws Exception {
        when(vehicleService.getVehiclePriceHistory("cars", 99, 99))
                .thenThrow(new ResourceNotFoundException("Recurso não encontrado na API FIPE."));

        mockMvc.perform(get("/api/v1/vehicles/99/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("não encontrado")));
    }

    @Test
    void shouldReturn502WhenFipeApiExceptionIsThrown() throws Exception {
        when(vehicleService.getVehiclePriceHistory("cars", 59, 5940))
                .thenThrow(new FipeApiException("A API FIPE retornou um erro interno: 503 SERVICE_UNAVAILABLE"));

        mockMvc.perform(get("/api/v1/vehicles/59/5940"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value(containsString("FIPE")));
    }

    @Test
    void shouldReturn500ForUnexpectedExceptions() throws Exception {
        when(vehicleService.getVehiclePriceHistory(any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/api/v1/vehicles/59/5940"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                // stacktrace não deve vazar para o cliente
                .andExpect(jsonPath("$.message").value("Ocorreu um erro interno inesperado."));
    }

    // -------------------------------------------------------------------------
    // Resposta com histórico vazio
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn200WithEmptyHistoryWhenNoYearsFound() throws Exception {
        when(vehicleService.getVehiclePriceHistory("cars", 59, 5940))
                .thenReturn(new VehicleHistoryResponse("", "", "", List.of()));

        mockMvc.perform(get("/api/v1/vehicles/59/5940"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceHistory", hasSize(0)));
    }
}
