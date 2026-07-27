package fu.se180211.department.repository;

import fu.se180211.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {
    boolean existsByCodeIgnoreCase(String v);

    boolean existsByCodeIgnoreCaseAndDepartmentIdNot(String v, Long id);
}
