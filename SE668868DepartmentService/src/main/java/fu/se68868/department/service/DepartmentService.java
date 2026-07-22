package fu.se68868.department.service;

import fu.se68868.department.dto.DepartmentDTO;
import fu.se68868.department.entity.Department;

public interface DepartmentService {

    //GỌI REPO ĐỂ CRUD TABLE Department
    //NHƯNG TRONG TAY CỦA NÓ CHỈ CÓ DTO NHẬN TỪ CONTROLLER
    //VÀ CONTROLLER NHẬN VỀ CŨNG CHỈ NHẬN DTO

    //REPO THÌ LẠI CHỈ CHƠI VỚI ENTITY -> MAPPER XUẤT HIỆN: CONVERT DTO <-> ENTITY

    //CÁC HÀM CRUD TABLE Department HERE; CHỈ CÓ TÊM HÀM HOY, IMPLMENTs BÊN CLASS Impl

    //1. làm trước hàm createDept(nhận vào deptDto từ FE)
    //                        -> return deptDto converted từ DB (có thêm key tự tăng)

    public DepartmentDTO createDept(DepartmentDTO feDto);

    //TODO: còn hàm R.U.D
}
