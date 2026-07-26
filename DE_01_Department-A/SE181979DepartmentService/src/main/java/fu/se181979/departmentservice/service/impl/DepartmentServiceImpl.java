package fu.se181979.departmentservice.service.impl;

import fu.se181979.departmentservice.dto.ApiResponseDTO;
import fu.se181979.departmentservice.dto.DepartmentDTO;
import fu.se181979.departmentservice.entity.Department;
import fu.se181979.departmentservice.repository.DepartmentRepository;
import fu.se181979.departmentservice.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;


    @Override
    public ApiResponseDTO createDepartment(DepartmentDTO dto) {
        Department entity = new  Department();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setLocation( dto.getLocation());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setParentId(dto.getParentId());

        entity.setStatus("ACTIVE");

        Department saveEntity = departmentRepository.save(entity);

        return new ApiResponseDTO(1, mapToDTO(saveEntity));


    }

    @Override
    public ApiResponseDTO getDepartmentDetail(Integer id) {
        Department entity = departmentRepository.findById(id).orElse(null);
        if (entity == null){
            return new ApiResponseDTO(4, null);

        }
        return new ApiResponseDTO(1, mapToDTO(entity));
    }

    private DepartmentDTO mapToDTO(Department entity){
        DepartmentDTO dto = new DepartmentDTO();

        dto.setDepartmentId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setLocation(entity.getLocation());
        dto.setStatus(entity.getStatus());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setParentId(entity.getParentId());

        return dto;
    }
}