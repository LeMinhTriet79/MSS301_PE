package fu.se180211.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fu.se180211.restaurant.common.FlexibleDateDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RestaurantDTO {
    private Long restaurantId;
    @NotBlank(message = "Name is mandatory")
    @Size(max = 50)
    private String name;
    @Size(max = 100)
    private String owner;
    @Size(max = 100)
    private String address;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    @JsonDeserialize(using = FlexibleDateDeserializer.class)
    private Date openDate;
    private Integer priceFrom;
    private Integer priceTo;
    @Size(max = 11)
    private String phone;
    @Size(max = 10)
    private String status;
    @NotNull(message = "CategoryId is mandatory")
    private Long categoryId;
}
