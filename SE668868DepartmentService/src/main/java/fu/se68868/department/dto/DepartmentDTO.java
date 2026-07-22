package fu.se68868.department.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

// class này dùng trong api CREAT, tạo mới 1 phòng ban (id, code, name,...)
// DTO này được gửi dưới dạng JSON từ React, Postman, SwaggerUI lên Controller
// DTO này xuất hiện trong HTTP REQUEST MESSAGE, được gửi từ Client lên Server
//==============================================================================
//class này dùng trong api CREATE, tạo mới 1 phòng ban (id, code, name,...)
//DTO này đc gửi dưới dạng JSON từ React, Postman, SwaggerUI lên Controller
//DTO này xuất hiện trong HTTP REQUEST MESSAGE, GỬI TỪ CLIENT LÊN SERVER
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DepartmentDTO {

    private Integer id;   //nếu tạo mới thì ko gửi id từ client lên server
    //nếu edit thì gửi từ server về client
    @NotNull(message = "Code is required")
    @Size(max = 10, message = "Code must not exceed 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Code only contains letters, digits")
    private String code;  //10, required, có format chỉ chữ, số -> Regular Expression (regex)

    @NotNull(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;  //50, required

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location; //100

    @Pattern(regexp = "ACTIVE|INACTIVE|CLOSED", message = "Invalid status. It must be: ACTIVE or INACTIVE or CLOSED")
    private String status;  //10 ACTIVE, INACTIVE, CLOSED

    @JsonFormat(pattern = "dd/MM/yyyy")  //dd-MM-yyyy hh:mm:ss
    private LocalDate effectiveDate;    //@DateTimeFormat dành cho <form> gửi lên server

    private Integer parentId;

    //constructor, getter, setter, toString() => Lombok

}