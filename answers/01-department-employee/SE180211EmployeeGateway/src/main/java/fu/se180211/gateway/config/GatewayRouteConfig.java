package fu.se180211.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("department-openapi", r -> r.path("/department-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/department-service/(?<path>.*)", "/${path}"))
                        .uri("http://localhost:8081"))
                .route("employee-openapi", r -> r.path("/employee-service/v3/api-docs")
                        .filters(f -> f.rewritePath("/employee-service/(?<path>.*)", "/${path}"))
                        .uri("http://localhost:8082"))
                .route("service-a", r -> r.path("/api/departments/**").uri("http://localhost:8081"))
                .route("service-b", r -> r.path("/api/employees/**").uri("http://localhost:8082"))
                .build();
    }
}
