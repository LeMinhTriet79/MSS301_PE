package fu.se180211.restaurant.service;

import fu.se180211.restaurant.dto.PageDTO;
import fu.se180211.restaurant.dto.RestaurantDTO;

public interface RestaurantService {
    RestaurantDTO create(RestaurantDTO dto);

    RestaurantDTO update(Long id, RestaurantDTO dto);

    RestaurantDTO get(Long id);

    void deactivate(Long id);

    PageDTO<RestaurantDTO> list(Integer page, Integer size, String name, String ownerName);
}
