package fu.se180211.employee.service;

import fu.se180211.employee.dto.EmployeeDTO;
import fu.se180211.employee.dto.PageDTO;

public interface EmployeeService {
    EmployeeDTO create(EmployeeDTO dto);

    EmployeeDTO update(Long id, EmployeeDTO dto);

    EmployeeDTO get(Long id);

    void deactivate(Long id);

    PageDTO<EmployeeDTO> list(Integer page, Integer size, String name, String status);
}
