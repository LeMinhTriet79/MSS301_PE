package fu.se180211.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DepartmentDTO {
    private Long departmentId;
    @Size(max = 10)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    private String code;
    @Size(max = 50)
    private String name;
    @Size(max = 100)
    private String location;
    @Size(max = 10)
    private String status;
    @JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
    private Date effectiveDate;
    private Long parentId;
}
