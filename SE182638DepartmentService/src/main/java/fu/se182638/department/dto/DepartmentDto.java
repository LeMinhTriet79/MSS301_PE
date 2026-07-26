package fu.se182638.department.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDto{

    private Integer id;

    @NotNull(message = "Code is required!")
    @Size(max = 10, message ="Code must not exceed 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Code only contains letter, characters")
    private String code; //10, required, có format ghi chữ với số -> regex

    @NotNull(message = "Name is required!")
    @Size(max = 50, message ="Name must not exceed 50 characters")
    private String name;//50, required

    @Size(max = 100, message ="Location must not exceed 100 characters")
    private String location;

    @Size(max = 10, message ="Status must not exceed 10 characters")
    @Pattern(regexp = "ACTIVE|INAVTIVE|CLOSED", message = "Inavalid status. It must be ACTIVE or INAVTIVE or CLOSED")
    private String status;

    @JsonFormat(pattern = "dd/MM/yyyy") //ddMMyyyy hh:mm:ss
    private LocalDate effectiveDate; //

    private Integer parentId;
}