package fu.se181979.departmentservice.service;

import fu.se181979.departmentservice.dto.ApiResponseDTO;
import fu.se181979.departmentservice.dto.DepartmentDTO;

public interface DepartmentService {
    ApiResponseDTO createDepartment(DepartmentDTO dto);
    ApiResponseDTO getDepartmentDetail(Integer id);
}
