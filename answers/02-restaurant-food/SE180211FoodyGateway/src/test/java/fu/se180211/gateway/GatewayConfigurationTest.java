package fu.se180211.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayConfigurationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void exposesAllRequiredServiceAndOpenApiRoutes() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes).extracting(Route::getId).contains(
                "restaurant-service", "food-service", "restaurant-openapi", "food-openapi");

        Route restaurantRoute = routes.stream()
                .filter(route -> route.getId().equals("restaurant-service"))
                .findFirst()
                .orElseThrow();
        ServerWebExchange categoryRequest = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/categories").build());
        assertThat(Mono.from(restaurantRoute.getPredicate().apply(categoryRequest)).block()).isTrue();
    }

    @Test
    void corsPreflightAcceptsAnyOrigin() {
        webTestClient.options()
                .uri("/api/restaurants")
                .header(HttpHeaders.ORIGIN, "https://example.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://example.test");
    }
}
