package com.kevdev.iam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI iasOpenApi() {
    SecurityScheme bearer = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");
    return new OpenAPI()
        .info(new Info().title("Identity Access Service").version("v1"))
        .schemaRequirement("bearerAuth", bearer)
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}

