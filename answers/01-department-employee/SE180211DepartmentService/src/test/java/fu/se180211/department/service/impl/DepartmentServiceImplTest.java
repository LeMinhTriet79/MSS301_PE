package fu.se180211.department.service.impl;

import fu.se180211.department.common.BusinessException;
import fu.se180211.department.common.ResponseStatuses;
import fu.se180211.department.dto.DepartmentDTO;
import fu.se180211.department.entity.Department;
import fu.se180211.department.repository.DepartmentRepository;
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
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository repository;

    private DepartmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepartmentServiceImpl(repository);
    }

    @Test
    void createUsesActiveAsDefaultAndReturnsSavedDepartment() {
        when(repository.existsByCodeIgnoreCase("HR01")).thenReturn(false);
        when(repository.save(any(Department.class))).thenAnswer(invocation -> {
            Department entity = invocation.getArgument(0);
            entity.setDepartmentId(1L);
            return entity;
        });

        DepartmentDTO result = service.create(validDepartment());

        assertThat(result.getDepartmentId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("HR01");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createRejectsDuplicateCodeWithStatusThree() {
        when(repository.existsByCodeIgnoreCase("HR01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validDepartment()))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.DUPLICATE_CODE);
                    assertThat(ex.getHttpStatus().value()).isEqualTo(400);
                });
    }

    @Test
    void createRejectsEffectiveDateBeforeTodayBecauseOfficialSqlRejectsIt() {
        DepartmentDTO dto = validDepartment();
        dto.setEffectiveDate(asDate(LocalDate.now().minusDays(1)));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.VALIDATION_FAILED));
    }

    @Test
    void createRejectsEffectiveDateAtOrAfterTodayPlus360() {
        DepartmentDTO dto = validDepartment();
        dto.setEffectiveDate(asDate(LocalDate.now().plusDays(360)));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.VALIDATION_FAILED));
    }

    @Test
    void getUnknownDepartmentReturnsStatusFourAndHttp400() {
        when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getApiStatus()).isEqualTo(ResponseStatuses.NOT_FOUND);
                    assertThat(ex.getHttpStatus().value()).isEqualTo(400);
                });
    }

    private DepartmentDTO validDepartment() {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setCode("HR01");
        dto.setName("Human Resources");
        dto.setLocation("Hoa Lac");
        dto.setEffectiveDate(asDate(LocalDate.now().plusDays(1)));
        return dto;
    }

    private Date asDate(LocalDate value) {
        return Date.from(value.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
