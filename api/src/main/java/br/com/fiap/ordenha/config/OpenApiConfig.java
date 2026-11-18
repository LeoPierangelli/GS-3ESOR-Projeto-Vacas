package br.com.fiap.ordenha.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Ordenha Monitorada API", version = "1.0",
                description = "Alertas de suspeita de mastite, queda de produção e leite em carência para o pequeno "
                        + "produtor de leite. Faça login em POST /auth/login, clique em Authorize e cole o token."),
        security = @SecurityRequirement(name = "token"))
@SecurityScheme(name = "token", type = SecuritySchemeType.HTTP, scheme = "bearer")
public class OpenApiConfig {
}
