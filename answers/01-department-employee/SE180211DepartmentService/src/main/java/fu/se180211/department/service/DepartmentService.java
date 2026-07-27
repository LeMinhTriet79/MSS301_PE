package fu.se180211.department.service;

import fu.se180211.department.dto.DepartmentDTO;
import fu.se180211.department.dto.PageDTO;

public interface DepartmentService {
    DepartmentDTO create(DepartmentDTO dto);

    DepartmentDTO update(Long id, DepartmentDTO dto);

    DepartmentDTO get(Long id);

    void deactivate(Long id);

    PageDTO<DepartmentDTO> list(Integer page, Integer size, String name, String status);
}
