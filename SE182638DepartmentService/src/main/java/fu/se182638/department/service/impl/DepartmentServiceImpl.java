package fu.se182638.department.service.impl;

import fu.se182638.department.dto.DepartmentDto;
import fu.se182638.department.entity.Department;
import fu.se182638.department.repository.DepartmentRepository;
import fu.se182638.department.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    // triệu hồi 1 repo giúp xuống tbl, nó chỉ chơi entity.Department
    @Autowired
    private DepartmentRepository deptRepo;

    @Override
    public DepartmentDto createDepartment(DepartmentDto feDto) {
        // trong repo thì hàm .save(entity) luôn trả về obj entitylaays từ db nếu save thành công

        // ta vẽ convert entity này -> dto mới, beDto chứ không phải feDto
        Department createRow = deptRepo.save(toEntity(feDto)); // chuyển feDto thành entity để dùng hàm .save()

        DepartmentDto beDto = toDto(createRow);

        return beDto;
    }

    //làm mapper - ánh xạ 2 chiều từ dto sang entity và ngc lại
    // có thể tách class riêng  mapper, có thể tách thêm package .mapper.
    //có thể dùng auto-mapping, thêm dependency để tự map giữa 2 bên

    // ta tự map bằng tay: new thằng sẽ tạo, set bên này = gtri get bên kia
    //                      từ entiy sang dto, từ db/tbl thành dto
    private DepartmentDto toDto(Department entity) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setLocation(entity.getLocation());
        dto.setStatus(entity.getStatus());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setParentId(entity.getParentId());

        return dto;
    }

    //                  từ dto sang entity, từ client thành entity dùng trong tbl
    private Department toEntity(DepartmentDto dept) {
        Department entity = new Department();
        entity.setId(dept.getId());
        entity.setCode(dept.getCode());
        entity.setName(dept.getName());
        entity.setLocation(dept.getLocation());
        entity.setStatus(dept.getStatus());
        entity.setEffectiveDate(dept.getEffectiveDate());
        entity.setParentId(dept.getParentId());

        return entity;
    }
}
