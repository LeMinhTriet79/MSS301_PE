package fu.se181979.employeeservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DepartmentDTO {
    private Integer departmentId;
    private String code;
    private String name;
    private String location;
    private String status;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate effectiveDate;
    private Integer parentId;


}