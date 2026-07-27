package fu.se180211.food.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RestaurantDTO {
    private Long restaurantId;
    @Size(max = 100)
    private String name;
    @Size(max = 100)
    private String owner;
    @Size(max = 100)
    private String address;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Date openDate;
    private Integer priceFrom;
    private Integer priceTo;
    @Size(max = 11)
    private String phone;
    @Size(max = 10)
    private String status;
    private Long categoryId;
}
