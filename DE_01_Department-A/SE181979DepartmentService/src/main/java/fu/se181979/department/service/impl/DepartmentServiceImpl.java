package fu.se181979.department.service.impl;

import fu.se181979.department.dto.ApiResponseDTO;
import fu.se181979.department.dto.DepartmentDTO;
import fu.se181979.department.entity.Department;
import fu.se181979.department.repository.DepartmentRepository;
import fu.se181979.department.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public ApiResponseDTO createDepartment(DepartmentDTO dto) {
        try {
            Department entity = new Department();
            entity.setCode(dto.getCode());
            entity.setName(dto.getName());
            entity.setLocation(dto.getLocation());
            entity.setEffectiveDate(dto.getEffectiveDate());
            entity.setParentId(dto.getParentId());
            entity.setStatus("ACTIVE"); // Mặc định là ACTIVE[cite: 16]

            Department saveEntity = departmentRepository.save(entity);
            return new ApiResponseDTO(1, mapToDTO(saveEntity)); // Thành công[cite: 16]
        } catch (Exception e) {
            return new ApiResponseDTO(0, null); // Lỗi server[cite: 16]
        }
    }

    @Override
    public ApiResponseDTO getDepartmentDetail(Integer id) {
        try {
            Department entity = departmentRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Không tìm thấy[cite: 16]
            }
            return new ApiResponseDTO(1, mapToDTO(entity));
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    @Override
    public ApiResponseDTO updateDepartment(Integer id, DepartmentDTO dto) {
        try {
            Department entity = departmentRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Không tìm thấy để update
            }

            // Chỉ cập nhật những trường có gửi lên
            if (dto.getCode() != null) entity.setCode(dto.getCode());
            if (dto.getName() != null) entity.setName(dto.getName());
            if (dto.getLocation() != null) entity.setLocation(dto.getLocation());
            if (dto.getEffectiveDate() != null) entity.setEffectiveDate(dto.getEffectiveDate());
            if (dto.getParentId() != null) entity.setParentId(dto.getParentId());
            if (dto.getStatus() != null) entity.setStatus(dto.getStatus());

            Department saveEntity = departmentRepository.save(entity);
            return new ApiResponseDTO(1, mapToDTO(saveEntity)); // Cập nhật thành công[cite: 16]
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    @Override
    public ApiResponseDTO deactivateDepartment(Integer id) {
        try {
            Department entity = departmentRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Không tìm thấy[cite: 16]
            }
            entity.setStatus("INACTIVE"); // Chuyển thành INACTIVE[cite: 16]
            departmentRepository.save(entity);
            return new ApiResponseDTO(1, null); // Xóa thành công[cite: 16]
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    private DepartmentDTO mapToDTO(Department entity) {
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