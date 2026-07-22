package fu.se68868.department.service.impl;

import fu.se68868.department.dto.DepartmentDTO;
import fu.se68868.department.entity.Department;
import fu.se68868.department.repository.DepartmentRepository;
import fu.se68868.department.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    //tiêm, triệu hồn repo giúp xuống table, nó chỉ chơi entity.Department
    @Autowired
    private DepartmentRepository deptRepo;

    @Override
    public DepartmentDTO createDept(DepartmentDTO feDto) {
        return null;
    }

    //LÀM MAPPER - ÁNH XẠ 2 CHIỀU TỪ DTO <----> ENTITY
    //CÓ THỂ TÁCH CLASS RIÊNG MAPPER, CÓ THỂ TÁCH PACKAGE .mapper.
    //CÓ THỂ XÀI AUTO-MAPPING, THÊM DEPENDENCY ĐỂ TỰ MAP GIỮA 2 BÊN

    //TA TỰ MAP BẰNG TAY: NEW THẰNG SẼ TẠO, SET BÊN NÀY = GIÁ TRỊ GET BÊN KIA

    //HÀM HELPER, TRỢ GIÚP CHO HÀM KHÁC TRONG CÙNG CLASS

    //                    từ entity sang dto, từ db/table thành dto
    private DepartmentDTO toDTO(Department entity) {

    }

    //                    từ dto sang entity, từ client thành entity dòng trong table
    private Department toEntity(DepartmentDTO dto) {

    }
}
