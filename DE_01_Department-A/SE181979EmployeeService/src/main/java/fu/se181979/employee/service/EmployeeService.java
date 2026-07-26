package fu.se181979.employee.service;

import fu.se181979.employee.dto.ApiResponseDTO;
import fu.se181979.employee.dto.EmployeeDTO;

public interface EmployeeService {
    ApiResponseDTO createEmployee(EmployeeDTO dto);
    ApiResponseDTO getEmployeeDetail(Integer id)   ;

    ApiResponseDTO updateEmployee(Integer id, EmployeeDTO dto);

    ApiResponseDTO deactivateEmployee(Integer id);
}
