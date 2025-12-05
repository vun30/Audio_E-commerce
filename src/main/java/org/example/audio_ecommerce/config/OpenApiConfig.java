package org.example.audio_ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
         // 🔥 Server Local
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Server");

        // 🔥  SERVER URL CHO RAILWAY
        Server productionServer = new Server()
                .url("https://audioe-commerce-production.up.railway.app")
                .description("Railway Production");

        Server baseServer = new Server()
                .url("https://audioe-commerce-production.up.railway.app")
                .description("Deployed Server");

        return new OpenAPI()
                .addServersItem(localServer)        // 👈 thêm local
                .addServersItem(productionServer) // ✔ Thêm server
                .addServersItem(baseServer)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .info(new Info().title("Audio System").version("v1"));
    }
}
