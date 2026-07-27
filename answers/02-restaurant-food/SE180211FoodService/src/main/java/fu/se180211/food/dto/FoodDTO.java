package fu.se180211.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodDTO {
    private Long foodId;
    @NotBlank(message = "Name is mandatory")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Price is mandatory")
    private Integer price;

    @NotBlank(message = "Ingredients is mandatory")
    @Size(max = 500)
    private String ingredients;

    @NotBlank(message = "Status is mandatory")
    @Size(max = 20)
    private String status;

    @NotNull(message = "RestaurantId is mandatory")
    private Long restaurantId;
}
