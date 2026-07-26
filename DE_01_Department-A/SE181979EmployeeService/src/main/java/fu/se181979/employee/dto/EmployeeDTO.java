package fu.se181979.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
@Data
public class EmployeeDTO {

    private Integer employeeId;
    private String fullName;
    private String position;
    private String status;
    private String email;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate endDate;

    // ĐIỂM ĂN TIỀN LÀ CHỖ NÀY: DTO yêu cầu trả về cả 1 object DepartmentDTO[cite: 3]
    private DepartmentDTO department;
}