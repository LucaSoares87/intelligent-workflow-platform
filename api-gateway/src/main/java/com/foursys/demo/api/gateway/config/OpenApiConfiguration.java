// ===== OpenApiConfiguration.java =====
package com.foursys.demo.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Intelligent Workflow Platform API")
                .version("0.1.0")
                .description("Plataforma de orquestração inteligente de processos com IA")
                .contact(new Contact()
                    .name("Foursys Tech Team")
                    .email("tech@foursys.com")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development"),
                new Server()
                    .url("https://api.workflow.foursys.com")
                    .description("Production")
            ));
    }
}