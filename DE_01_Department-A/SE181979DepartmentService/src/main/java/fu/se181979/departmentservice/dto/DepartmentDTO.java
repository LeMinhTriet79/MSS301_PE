package fu.se181979.departmentservice.dto;

import java.util.Date;

public class DepartmentDTO {
    private Long departmentId;
    private String code;
    private String name;
    private String location;
    private String status;
    private Date effectiveDate;
    private Long parentId;

    // Em nhớ dùng IntelliJ Generate (Alt + Insert) toàn bộ Getter và Setter ở đây nhé!
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(Date effectiveDate) { this.effectiveDate = effectiveDate; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}