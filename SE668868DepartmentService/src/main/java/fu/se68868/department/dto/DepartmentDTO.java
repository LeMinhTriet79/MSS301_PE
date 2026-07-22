package fu.se68868.department.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

// class này dùng trong api CREAT, tạo mới 1 phòng ban (id, code, name,...)
// DTO này được gửi dưới dạng JSON từ React, Postman, SwaggerUI lên Controller
// DTO này xuất hiện trong HTTP REQUEST MESSAGE, được gửi từ Client lên Server
public class DepartmentDTO {

    private Integer id; //nếu tạo mới thì không gửi ID từ client lên, nếu edit thì gửi từ service về client

    private String name; // 50, required

    private String code; // 10, required

    private LocalDate effectiveDate;


    private String status;


    private String location;


    private Integer parentId;

}