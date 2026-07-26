package fu.se181979.department.service;

import fu.se181979.department.dto.ApiResponseDTO;
import fu.se181979.department.dto.DepartmentDTO;

public interface DepartmentService {
    ApiResponseDTO createDepartment(DepartmentDTO dto);
    ApiResponseDTO getDepartmentDetail(Integer id);

    ApiResponseDTO updateDepartment(Integer id, DepartmentDTO dto);

    ApiResponseDTO deactivateDepartment(Integer id);
}
