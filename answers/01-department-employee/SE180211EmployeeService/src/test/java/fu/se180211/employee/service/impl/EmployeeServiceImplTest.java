package fu.se180211.employee.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se180211.employee.common.BusinessException;
import fu.se180211.employee.common.ResponseStatuses;
import fu.se180211.employee.config.DepartmentClient;
import fu.se180211.employee.dto.ApiResponseDTO;
import fu.se180211.employee.dto.DepartmentDTO;
import fu.se180211.employee.dto.EmployeeDTO;
import fu.se180211.employee.entity.Employee;
import fu.se180211.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private DepartmentClient departmentClient;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(repository, departmentClient, new ObjectMapper());
    }

    @Test
    void createValidatesDepartmentAndUsesActiveAsDefault() {
        DepartmentDTO department = department(10L);
        when(departmentClient.getDepartment(10L)).thenReturn(ApiResponseDTO.success(department));
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee entity = invocation.getArgument(0);
            entity.setEmployeeId(1L);
            return entity;
        });

        EmployeeDTO result = service.create(validEmployee());

        assertThat(result.getEmployeeId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getDepartment().getDepartmentId()).isEqualTo(10L);
    }

    @Test
    void createRejectsUnknownDepartmentWithStatusFour() {
        when(departmentClient.getDepartment(10L))
                .thenReturn(ApiResponseDTO.of(ResponseStatuses.NOT_FOUND, null));

        assertThatThrownBy(() -> service.create(validEmployee()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.NOT_FOUND);
                    assertThat(ex.getHttpStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        EmployeeDTO dto = validEmployee();
        dto.setEndDate(asDate(LocalDate.of(2026, 3, 1)));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.VALIDATION_FAILED));
    }

    @Test
    void updateRejectsPresentDepartmentWithoutId() {
        Employee current = new Employee();
        current.setEmployeeId(1L);
        current.setDepartmentId(10L);
        current.setStartDate(asDate(LocalDate.of(2026, 3, 10)));
        when(repository.findById(1L)).thenReturn(java.util.Optional.of(current));

        EmployeeDTO patch = new EmployeeDTO();
        patch.setDepartment(new DepartmentDTO());

        assertThatThrownBy(() -> service.update(1L, patch))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.VALIDATION_FAILED));
    }

    @Test
    void getUnknownEmployeeReturnsStatusFourAndHttp400() {
        when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.NOT_FOUND);
                    assertThat(ex.getHttpStatus().value()).isEqualTo(400);
                });
    }

    private EmployeeDTO validEmployee() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setFullName("Nguyen Van An");
        dto.setEmail("an@example.com");
        dto.setPosition("Developer");
        dto.setStartDate(asDate(LocalDate.of(2026, 3, 10)));
        dto.setDepartment(department(10L));
        return dto;
    }

    private DepartmentDTO department(Long id) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDepartmentId(id);
        dto.setCode("IT01");
        dto.setName("Information Technology");
        return dto;
    }

    private Date asDate(LocalDate value) {
        return Date.from(value.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
