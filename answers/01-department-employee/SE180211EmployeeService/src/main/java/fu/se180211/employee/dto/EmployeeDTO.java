package fu.se180211.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class EmployeeDTO {
    private Long employeeId;
    @NotBlank(message = "FullName is mandatory")
    @Size(max = 100)
    private String fullName;
    @NotBlank(message = "Email is mandatory")
    @Size(max = 100)
    @Email
    private String email;
    @NotBlank(message = "Position is mandatory")
    @Size(max = 30)
    private String position;
    @NotBlank(message = "Status is mandatory")
    @Size(max = 10)
    private String status;
    @NotNull(message = "StartDate is mandatory")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Date endDate;
    private DepartmentDTO department;
}
