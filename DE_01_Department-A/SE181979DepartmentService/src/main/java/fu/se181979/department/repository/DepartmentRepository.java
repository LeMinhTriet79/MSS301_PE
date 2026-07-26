package fu.se181979.department.repository;

import fu.se181979.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    boolean existsByCode(String code);
}