package fu.se180211.food.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodResponseDTO {
    private Long foodId;
    private String name;
    private Integer price;
    private String ingredients;
    private RestaurantDTO restaurant;
}
