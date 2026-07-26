package fu.se181979.employee.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se181979.employee.config.DepartmentClient;
import fu.se181979.employee.dto.ApiResponseDTO;
import fu.se181979.employee.dto.DepartmentDTO;
import fu.se181979.employee.dto.EmployeeDTO;
import fu.se181979.employee.entity.Employee;
import fu.se181979.employee.repository.EmployeeRepository;
import fu.se181979.employee.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    DepartmentClient departmentClient;

    @Override
    public ApiResponseDTO createEmployee(EmployeeDTO dto) {
        try {
            Integer depId = null;
            if(dto.getDepartment() != null) {
                depId = dto.getDepartment().getDepartmentId();
            }
            ApiResponseDTO deptResponse = departmentClient.getDepartmentByID(depId);
            if (deptResponse == null || deptResponse.getStatus() != 1) {
                return new ApiResponseDTO(4, null); // Status 4: Department ID is not found[cite: 16]
            }

            Employee employee = new Employee();
            employee.setFullName(dto.getFullName());
            employee.setEmail(dto.getEmail());
            employee.setPosition(dto.getPosition());
            employee.setStartDate(dto.getStartDate());
            employee.setEndDate(dto.getEndDate());
            employee.setDepartmentId(depId);
            employee.setStatus("ACTIVE"); // Mặc định ACTIVE[cite: 16]

            Employee savedEntity = employeeRepository.save(employee);
            return new ApiResponseDTO(1, mapToDTO(savedEntity, deptResponse.getData()));
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    @Override
    public ApiResponseDTO getEmployeeDetail(Integer id) {
        try {
            Employee entity = employeeRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Không tìm thấy[cite: 16]
            }

            ApiResponseDTO deptResponse = departmentClient.getDepartmentByID(entity.getDepartmentId());
            Object deptData = (deptResponse != null && deptResponse.getStatus() == 1) ? deptResponse.getData() : null;

            return new ApiResponseDTO(1, mapToDTO(entity, deptData));
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    @Override
    public ApiResponseDTO updateEmployee(Integer id, EmployeeDTO dto) {
        try {
            Employee entity = employeeRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Lỗi ID[cite: 16]
            }

            // Gọi Feign nếu user gửi department ID mới
            Object deptData = null;
            if (dto.getDepartment() != null && dto.getDepartment().getDepartmentId() != null) {
                Integer depId = dto.getDepartment().getDepartmentId();
                ApiResponseDTO deptResponse = departmentClient.getDepartmentByID(depId);
                if (deptResponse == null || deptResponse.getStatus() != 1) {
                    return new ApiResponseDTO(4, null); // Status 4: Dept Not found[cite: 16]
                }
                entity.setDepartmentId(depId);
                deptData = deptResponse.getData();
            } else {
                ApiResponseDTO deptResponse = departmentClient.getDepartmentByID(entity.getDepartmentId());
                deptData = (deptResponse != null && deptResponse.getStatus() == 1) ? deptResponse.getData() : null;
            }

            if(dto.getFullName() != null) entity.setFullName(dto.getFullName());
            if(dto.getEmail() != null) entity.setEmail(dto.getEmail());
            if(dto.getPosition() != null) entity.setPosition(dto.getPosition());
            if(dto.getStartDate() != null) entity.setStartDate(dto.getStartDate());
            if(dto.getEndDate() != null) entity.setEndDate(dto.getEndDate());
            if(dto.getStatus() != null) entity.setStatus(dto.getStatus());

            Employee savedEntity = employeeRepository.save(entity);
            return new ApiResponseDTO(1, mapToDTO(savedEntity, deptData));
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    @Override
    public ApiResponseDTO deactivateEmployee(Integer id) {
        try {
            Employee entity = employeeRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Không tìm thấy[cite: 16]
            }
            entity.setStatus("INACTIVE"); // Hủy kích hoạt[cite: 16]
            employeeRepository.save(entity);
            return new ApiResponseDTO(1, null);
        } catch (Exception e) {
            return new ApiResponseDTO(0, null);
        }
    }

    private EmployeeDTO mapToDTO(Employee entity, Object deptData){
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setEmail(entity.getEmail());
        dto.setPosition(entity.getPosition());
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        if (deptData != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            DepartmentDTO deptDto = mapper.convertValue(deptData, DepartmentDTO.class);
            dto.setDepartment(deptDto);
        }
        return dto;
    }
}