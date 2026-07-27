package fu.se180211.restaurant.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {
    private Long categoryId;
    @Size(max = 100)
    private String name;
}
