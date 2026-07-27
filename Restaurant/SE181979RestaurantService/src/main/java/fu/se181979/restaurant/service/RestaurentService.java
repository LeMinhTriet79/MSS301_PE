package fu.se181979.restaurant.service;

import fu.se181979.restaurant.dto.ApiResponse;
import fu.se181979.restaurant.dto.RestaurantDTO;

public interface RestaurentService {

    public abstract ApiResponse createRestaurant(RestaurantDTO dto);
    public ApiResponse getRestaurantDetail(Integer id);
}
