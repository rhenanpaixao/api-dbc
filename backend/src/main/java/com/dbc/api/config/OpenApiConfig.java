package com.dbc.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FIPE Vehicle Price History API")
                        .version("1.0")
                        .description("Serviço REST para consulta do histórico de preços de veículos via Tabela FIPE. " +
                                "Recebe o ID da marca e do modelo e retorna o valor e o percentual de variação ao longo dos anos de fabricação.")
                        .contact(new Contact().name("DBC Company")));
    }
}
