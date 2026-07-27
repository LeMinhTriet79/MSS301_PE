package fu.se181979.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
public class RestaurantDTO {
    long restaurantId;
    String name;
    private String ownerName;
    private Integer priceFrom;
    private Integer priceTo;
    private String phone;
    private String address;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date openDate;
    private String status;

    private Integer categoryId;

}