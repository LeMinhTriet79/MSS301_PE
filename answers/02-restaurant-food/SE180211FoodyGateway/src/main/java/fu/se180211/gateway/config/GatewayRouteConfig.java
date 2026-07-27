package fu.se180211.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    private final String restaurantServiceUrl;
    private final String foodServiceUrl;

    public GatewayRouteConfig(
            @Value("${restaurant.service.url:http://localhost:8081}") String restaurantServiceUrl,
            @Value("${food.service.url:http://localhost:8082}") String foodServiceUrl) {
        this.restaurantServiceUrl = restaurantServiceUrl;
        this.foodServiceUrl = foodServiceUrl;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("restaurant-service", r -> r
                        .path("/api/restaurants/**", "/api/categories/**")
                        .uri(restaurantServiceUrl))
                .route("food-service", r -> r
                        .path("/api/foods/**")
                        .uri(foodServiceUrl))
                .route("restaurant-openapi", r -> r
                        .path("/restaurant-service/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(restaurantServiceUrl))
                .route("food-openapi", r -> r
                        .path("/food-service/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(foodServiceUrl))
                .build();
    }
}
