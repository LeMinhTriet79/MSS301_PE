package fu.se180211.food.service;

import fu.se180211.food.dto.FoodDTO;
import fu.se180211.food.dto.FoodListDTO;
import fu.se180211.food.dto.FoodResponseDTO;

public interface FoodService {
    FoodDTO create(FoodDTO dto);

    FoodDTO update(Long id, FoodDTO dto);

    FoodResponseDTO get(Long id);

    void deactivate(Long id);

    FoodListDTO list(Integer page, Integer size, String name, String ingredients);
}
