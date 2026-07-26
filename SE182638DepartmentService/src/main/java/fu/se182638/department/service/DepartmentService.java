package fu.se182638.department.service;

import fu.se182638.department.dto.DepartmentDto;

public interface DepartmentService {

    //gọi repo để crud tbl department
    //nhưng trong tay của nó chỉ có dto nhận từ controller
    //và controller nhận về cũng chỉ nhận dto

    //repo thì lại chỉ chơi entity -> mapper xuất hiện : convert dto <-> entity

    //các hàm crud tbl department

    public DepartmentDto createDepartment(DepartmentDto feDto);
}
