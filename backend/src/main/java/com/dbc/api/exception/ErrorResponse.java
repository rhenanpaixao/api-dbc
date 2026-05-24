package com.dbc.api.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta padrão de erro")
public record ErrorResponse(
        @Schema(description = "Código HTTP", example = "404")
        int status,

        @Schema(description = "Mensagem descritiva do erro", example = "Recurso não encontrado na API FIPE")
        String message,

        @Schema(description = "Momento em que o erro ocorreu")
        LocalDateTime timestamp
) {}
