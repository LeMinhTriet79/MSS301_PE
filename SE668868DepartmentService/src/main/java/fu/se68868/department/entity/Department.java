package fu.se68868.department.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Nationalized
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Size(max = 10)
    @NotNull
    @Nationalized
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Size(max = 10)
    @Nationalized
    @Column(name = "status", length = 10)
    private String status;

    @Size(max = 100)
    @Nationalized
    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "parent_id")
    private Integer parentId;


}