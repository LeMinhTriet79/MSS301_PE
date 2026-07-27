package fu.se180211.food.config;

import fu.se180211.food.dto.ApiResponseDTO;
import fu.se180211.food.dto.RestaurantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service", url = "${restaurant.service.url:http://localhost:8081}")
public interface RestaurantClient {
    @GetMapping("/api/restaurants/{restaurantId}")
    ApiResponseDTO<RestaurantDTO> getRestaurant(@PathVariable("restaurantId") Long restaurantId);
}
