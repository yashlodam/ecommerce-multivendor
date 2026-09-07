package com.zosh.config;

import java.util.Collections;
import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;

/**
 * OpenAPI 3.0 / Swagger UI configuration for ShopSphere.
 *
 * Configured specifically for ShopSphere's Dual-Token Architecture:
 * 1. Short-Lived Access Token (15 mins):
 *    Returned in JSON response upon login/signup and used to authenticate protected endpoints
 *    via HTTP Authorization header: "Authorization: Bearer <access-token>".
 * 2. Refresh Token (7 days):
 *    Stored in HttpOnly SameSite cookie ("refreshToken") and automatically transmitted by the browser
 *    for silent token rotation at POST /auth/refresh and session revocation at POST /auth/logout.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ShopSphere — Multi-Vendor Marketplace REST API",
        version = "1.0.0",
        description = "### ShopSphere Multi-Vendor Marketplace API\n\n"
            + "#### Authentication Architecture (Dual-Token System):\n"
            + "- **Short-Lived Access Token (15 minutes)**: Returned in login/signup JSON (`jwt`), stored in memory by frontend clients, and validated via `Authorization: Bearer <token>`.\n"
            + "- **Refresh Token (7 days)**: Set in an `HttpOnly`, `SameSite=Lax` cookie (`refreshToken`) with database persistence, rotation, and replay attack defense.\n\n"
            + "#### Swagger UI Authentication Guide:\n"
            + "1. **Login**: Call `POST /auth/login` (or `/auth/signup`). Your browser receives and retains the HttpOnly `refreshToken` cookie, while the JSON body returns `{ \"jwt\": \"<access_token>\", ... }`.\n"
            + "2. **Authorize**: Copy the `jwt` token string, click the green **Authorize** button above, paste the token into the `BearerAuth` input field, and click **Authorize**.\n"
            + "3. **Protected APIs**: All protected endpoints (`/api/users/profile`, `/api/cart`, `/sellers/profile`, etc.) now execute successfully.\n"
            + "4. **Token Refresh**: Call `POST /auth/refresh`. Because Swagger UI is hosted on the same origin (`http://localhost:5454`), your browser automatically sends the `refreshToken` cookie. A new access token is returned.\n"
            + "5. **Logout**: Call `POST /auth/logout`. The browser sends the cookie, the server revokes it, and clears the cookie from your browser.",
        contact = @Contact(
            name = "ShopSphere Engineering",
            email = "support@shopsphere.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "/", description = "Default Server URL")
    }
)
@SecurityScheme(
    name = "BearerAuth",
    description = "JWT Access Token for protected endpoints. Enter the 'jwt' string returned by POST /auth/login. Swagger UI automatically prefixes 'Bearer '.",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class OpenApiConfig {

    /**
     * Customizes OpenAPI operations to apply BearerAuth only to protected endpoints,
     * ensuring public endpoints display cleanly without lock icons.
     */
    @Bean
    public OpenApiCustomizer openApiSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (isPublicEndpoint(path, httpMethod.name())) {
                        operation.setSecurity(Collections.emptyList());
                    } else {
                        operation.setSecurity(List.of(new SecurityRequirement().addList("BearerAuth")));
                    }
                });
            });
        };
    }

    private boolean isPublicEndpoint(String path, String httpMethod) {
        if (httpMethod.equalsIgnoreCase("OPTIONS")) return true;
        if (path.startsWith("/auth/")) return true;
        if (httpMethod.equalsIgnoreCase("GET") && (path.startsWith("/products") || path.startsWith("/home") || path.equals("/"))) return true;
        if (httpMethod.equalsIgnoreCase("GET") && path.contains("/reviews")) return true;
        if (path.startsWith("/api/chat")) return true;
        if (path.equals("/health") || path.startsWith("/actuator/health")) return true;
        if (httpMethod.equalsIgnoreCase("POST") && (path.equals("/sellers/login") || path.equals("/sellers"))) return true;
        if (httpMethod.equalsIgnoreCase("PATCH") && path.startsWith("/sellers/verify")) return true;
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) return true;

        return false;
    }
}

