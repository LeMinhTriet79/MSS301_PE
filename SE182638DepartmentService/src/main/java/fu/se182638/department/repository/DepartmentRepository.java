package fu.se182638.department.repository;

import fu.se182638.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    // chứa hàm crud tbl dept, thông qua entity class entity.Department
    // chứa thêm hàm độ riêng không có sẵn
}