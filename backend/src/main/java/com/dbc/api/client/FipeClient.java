package com.dbc.api.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.dbc.api.dto.BrandDto;
import com.dbc.api.dto.FipeInfoDto;
import com.dbc.api.dto.ModelDto;
import com.dbc.api.dto.YearDto;
import com.dbc.api.exception.FipeApiException;
import com.dbc.api.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Responsável exclusivamente por se comunicar com a API pública FIPE.
 * Toda lógica de negócio fica na camada de serviço.
 */
@Component
@RequiredArgsConstructor
public class FipeClient {

    private final RestClient fipeRestClient;

    public List<BrandDto> getBrands(String vehicleType) {
        return get(
                "/{vehicleType}/brands",
                new ParameterizedTypeReference<>() {},
                vehicleType
        );
    }

    public List<ModelDto> getModels(String vehicleType, int brandId) {
        return get(
                "/{vehicleType}/brands/{brandId}/models",
                new ParameterizedTypeReference<>() {},
                vehicleType, brandId
        );
    }

    public List<YearDto> getYears(String vehicleType, int brandId, int modelId) {
        return get(
                "/{vehicleType}/brands/{brandId}/models/{modelId}/years",
                new ParameterizedTypeReference<>() {},
                vehicleType, brandId, modelId
        );
    }

    public FipeInfoDto getFipeInfo(String vehicleType, int brandId, int modelId, String yearId) {
        return get(
                "/{vehicleType}/brands/{brandId}/models/{modelId}/years/{yearId}",
                new ParameterizedTypeReference<>() {},
                vehicleType, brandId, modelId, yearId
        );
    }

    private <T> T get(String uriTemplate, ParameterizedTypeReference<T> responseType, Object... uriVars) {
        try {
            return fipeRestClient.get()
                    .uri(uriTemplate, uriVars)
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(
                    "Recurso não encontrado na API FIPE. Verifique os IDs informados.");
        } catch (HttpClientErrorException e) {
            throw new FipeApiException(
                    "Erro ao consultar a API FIPE: " + e.getStatusCode());
        } catch (HttpServerErrorException e) {
            throw new FipeApiException(
                    "A API FIPE retornou um erro interno: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            throw new FipeApiException(
                    "Não foi possível conectar à API FIPE. Verifique sua internet.");
        }
    }
}
