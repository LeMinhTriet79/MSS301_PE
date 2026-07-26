package fu.se181979.employeeservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se181979.employeeservice.config.DepartmentClient;
import fu.se181979.employeeservice.dto.ApiResponseDTO;
import fu.se181979.employeeservice.dto.DepartmentDTO;
import fu.se181979.employeeservice.dto.EmployeeDTO;
import fu.se181979.employeeservice.entity.Employee;
import fu.se181979.employeeservice.repository.EmployeeRepository;
import fu.se181979.employeeservice.service.EmployeeService;
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
        Integer depId = null;
        if(dto.getDepartment() != null) {
            depId = dto.getDepartment().getDepartmentId();
        }
        ApiResponseDTO deptResponse = departmentClient.getDepartmentByID(depId);
        if (deptResponse == null || deptResponse.getStatus() != 1) {
            return new ApiResponseDTO(4, null); // Status 4: Department ID is not found
        }
        Employee employee = new Employee();
        employee.setFullName(dto.getFullName());
        employee.setEmail(dto.getEmail());
        employee.setPosition(dto.getPosition());
        employee.setStartDate(dto.getStartDate());
        employee.setEndDate(dto.getEndDate());
        employee.setDepartmentId(depId);
        employee.setStatus("ACTIVE");

        Employee savedEntity = employeeRepository.save(employee);
        return new ApiResponseDTO(1, mapToDTO(savedEntity, deptResponse.getData()));
    }

    @Override
    public ApiResponseDTO getEmployeeDetail(Integer id) {
        try {
            Employee entity = employeeRepository.findById(id).orElse(null);
            if (entity == null) {
                return new ApiResponseDTO(4, null); // Status 4: Không tìm thấy nhân viên[cite: 3]
            }

            // Gọi qua port 8081 lấy cục data phòng ban về
            ApiResponseDTO deptResponse = departmentClient.getDepartmentByID(entity.getDepartmentId());
            Object deptData = (deptResponse != null && deptResponse.getStatus() == 1) ? deptResponse.getData() : null;

            return new ApiResponseDTO(1, mapToDTO(entity, deptData));
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
            DepartmentDTO deptDto = mapper.convertValue(deptData, DepartmentDTO.class);
            dto.setDepartment(deptDto);
        }
        return dto;
    }
}
