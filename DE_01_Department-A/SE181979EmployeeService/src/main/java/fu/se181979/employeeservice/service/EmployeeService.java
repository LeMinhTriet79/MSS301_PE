package fu.se181979.employeeservice.service;

import fu.se181979.employeeservice.dto.ApiResponseDTO;
import fu.se181979.employeeservice.dto.EmployeeDTO;
import fu.se181979.employeeservice.entity.Employee;

public interface EmployeeService {
    ApiResponseDTO createEmployee(EmployeeDTO dto);
    ApiResponseDTO getEmployeeDetail(Integer id)   ;
}
