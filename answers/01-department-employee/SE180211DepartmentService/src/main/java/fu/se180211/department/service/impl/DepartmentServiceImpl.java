package fu.se180211.department.service.impl;

import fu.se180211.department.common.BusinessException;
import fu.se180211.department.common.ResponseStatuses;
import fu.se180211.department.dto.DepartmentDTO;
import fu.se180211.department.dto.PageDTO;
import fu.se180211.department.entity.Department;
import fu.se180211.department.repository.DepartmentRepository;
import fu.se180211.department.service.DepartmentService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> STATUS_VALUES = Set.of("ACTIVE", "INACTIVE", "CLOSED");
    private static final String STATUS_DEFAULT = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    private final DepartmentRepository repo;

    public DepartmentServiceImpl(DepartmentRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public DepartmentDTO create(DepartmentDTO dto) {
        validateCreate(dto);
        String _u = clean(dto.getCode());
        if (repo.existsByCodeIgnoreCase(_u)) throw duplicate();
        Department e = new Department();
        e.setCode(_u);
        e.setName(clean(dto.getName()));
        e.setLocation(cleanNullable(dto.getLocation()));
        e.setEffectiveDate(dto.getEffectiveDate());
        e.setParentId(dto.getParentId());
        e.setStatus(STATUS_DEFAULT);
        return toDTO(repo.save(e));
    }

    @Override
    @Transactional
    public DepartmentDTO update(Long id, DepartmentDTO dto) {
        Department e = findOrThrow(id);
        validateUpdate(dto);
        if (dto.getCode() != null) {
            String candidate = clean(dto.getCode());
            if (repo.existsByCodeIgnoreCaseAndDepartmentIdNot(candidate, id)) throw duplicate();
        }
        if (dto.getCode() != null) e.setCode(clean(dto.getCode()));
        if (dto.getName() != null) e.setName(clean(dto.getName()));
        if (dto.getLocation() != null) e.setLocation(cleanNullable(dto.getLocation()));
        if (dto.getStatus() != null) e.setStatus(normalizeStatus(dto.getStatus()));
        if (dto.getEffectiveDate() != null) e.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getParentId() != null) e.setParentId(dto.getParentId());
        return toDTO(repo.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO get(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Department e = findOrThrow(id);
        e.setStatus(STATUS_INACTIVE);
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<DepartmentDTO> list(Integer page, Integer size, String name, String status) {
        int p = page == null ? 0 : page;
        int sz = size == null ? 10 : size;
        validatePagination(p, sz);
        String filterName = (name == null || name.isBlank()) ? null : name.trim().toLowerCase(Locale.ROOT);
        String filterStatus = (status == null || status.isBlank()) ? null : normalizeStatus(status);
        Specification<Department> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (filterName != null) ps.add(cb.like(cb.lower(root.get("name")), "%" + filterName + "%"));
            if (filterStatus != null) ps.add(cb.equal(root.get("status"), filterStatus));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<DepartmentDTO> result = repo.findAll(spec, PageRequest.of(p, sz, Sort.by("departmentId").ascending()))
                .map(this::toDTO);
        return new PageDTO<>(result);
    }

    private void validateCreate(DepartmentDTO dto) {
        if (dto == null) throw validationFailed();
        validateRequiredString(dto.getCode(), 10);
        validatePattern(dto.getCode(), CODE_PATTERN);
        validateRequiredString(dto.getName(), 50);
        validateOptionalString(dto.getLocation(), 100);
        validateStatusIfPresent(dto.getStatus());
        validateEffectiveDate(dto.getEffectiveDate());
        validateParent(dto.getParentId());
    }

    private void validateUpdate(DepartmentDTO dto) {
        if (dto == null) throw validationFailed();
        if (dto.getCode() != null) {
            validateRequiredString(dto.getCode(), 10);
            validatePattern(dto.getCode(), CODE_PATTERN);
        }
        if (dto.getName() != null) {
            validateRequiredString(dto.getName(), 50);
        }
        if (dto.getLocation() != null) {
            validateOptionalString(dto.getLocation(), 100);
        }
        validateStatusIfPresent(dto.getStatus());
        if (dto.getEffectiveDate() != null) validateEffectiveDate(dto.getEffectiveDate());
        if (dto.getParentId() != null) validateParent(dto.getParentId());
    }

    private Department findOrThrow(Long id) {
        if (id == null || id <= 0) throw notFound();
        return repo.findById(id).orElseThrow(this::notFound);
    }

    private DepartmentDTO toDTO(Department e) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDepartmentId(e.getDepartmentId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setLocation(e.getLocation());
        dto.setStatus(e.getStatus());
        dto.setEffectiveDate(e.getEffectiveDate());
        dto.setParentId(e.getParentId());
        return dto;
    }

    private void validateRequiredString(String v, int max) {
        if (v == null || v.trim().isEmpty() || (max > 0 && v.trim().length() > max)) throw validationFailed();
    }

    private void validateOptionalString(String v, int max) {
        if (v != null && max > 0 && v.trim().length() > max) throw validationFailed();
    }

    private void validatePattern(String v, Pattern p) {
        if (v != null && !p.matcher(v.trim()).matches()) throw validationFailed();
    }

    private String normalizeStatus(String x) {
        String n = clean(x).toUpperCase(Locale.ROOT);
        if (!STATUS_VALUES.contains(n)) throw validationFailed();
        return n;
    }

    private void validateStatusIfPresent(String x) {
        if (x != null) normalizeStatus(x);
    }

    private void validateEffectiveDate(java.util.Date value) {
        if (value == null) return;
        LocalDate date = toLocal(value);
        LocalDate today = LocalDate.now();
        // The paper says after 2000-01-01 and before today + 360 days. The
        // supplied SQL additionally rejects dates before today, so enforcing
        // both here prevents a valid-looking request from failing in SQL Server.
        if (!date.isAfter(LocalDate.of(2000, 1, 1))
                || date.isBefore(today)
                || !date.isBefore(today.plusDays(360))) {
            throw validationFailed();
        }
    }

    private void validateParent(Long parentId) {
        if (parentId == null) return; // Company-level departments have no parent.
        // The supplied SQL does not declare a FOREIGN KEY for parent_id and the
        // endpoint table does not define a "parent not found" response. Do not
        // invent that business rule; only reject a non-positive identifier.
        if (parentId <= 0) {
            throw validationFailed();
        }
    }

    private LocalDate toLocal(java.util.Date d) {
        return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void validatePagination(int p, int sz) {
        if (p < 0 || sz < 1 || sz > MAX_PAGE_SIZE) throw validationFailed();
    }

    private String clean(String v) {
        return v == null ? null : v.trim();
    }

    private String cleanNullable(String v) {
        String c = clean(v);
        return (c == null || c.isEmpty()) ? null : c;
    }

    private BusinessException validationFailed() {
        return new BusinessException(ResponseStatuses.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Data validation failed");
    }

    private BusinessException duplicate() {
        return new BusinessException(ResponseStatuses.DUPLICATE_CODE, HttpStatus.BAD_REQUEST, "Code is duplicated");
    }

    private BusinessException notFound() {
        return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "Department is not found");
    }
}
