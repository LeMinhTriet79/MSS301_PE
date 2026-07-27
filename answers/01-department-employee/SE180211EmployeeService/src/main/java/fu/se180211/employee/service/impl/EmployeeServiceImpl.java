package fu.se180211.employee.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import fu.se180211.employee.common.BusinessException;
import fu.se180211.employee.common.ResponseStatuses;
import fu.se180211.employee.config.DepartmentClient;
import fu.se180211.employee.dto.ApiResponseDTO;
import fu.se180211.employee.dto.DepartmentDTO;
import fu.se180211.employee.dto.EmployeeDTO;
import fu.se180211.employee.dto.PageDTO;
import fu.se180211.employee.entity.Employee;
import fu.se180211.employee.repository.EmployeeRepository;
import fu.se180211.employee.service.EmployeeService;
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
public class EmployeeServiceImpl implements EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> STATUS_VALUES = Set.of("LEFT", "RETIRED", "ACTIVE", "INACTIVE");
    private static final String STATUS_DEFAULT = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Set<String> POSITION_VALUES = Set.of("Manager", "Developer", "Staff");

    private final EmployeeRepository repo;
    private final DepartmentClient refClient;
    private final ObjectMapper mapper;

    public EmployeeServiceImpl(EmployeeRepository repo, DepartmentClient refClient, ObjectMapper mapper) {
        this.repo = repo;
        this.refClient = refClient;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {
        validateCreate(dto);
        Long _refId = resolveRefId(dto, true);
        DepartmentDTO refObj = fetchRef(_refId);
        Employee e = new Employee();
        e.setFullName(clean(dto.getFullName()));
        e.setEmail(clean(dto.getEmail()));
        e.setPosition(normalizeEnum(dto.getPosition(), POSITION_VALUES));
        e.setStartDate(dto.getStartDate());
        e.setEndDate(dto.getEndDate());
        e.setStatus(STATUS_DEFAULT);
        e.setDepartmentId(refObj.getDepartmentId());
        return toDTO(repo.save(e), refObj);
    }

    @Override
    @Transactional
    public EmployeeDTO update(Long id, EmployeeDTO dto) {
        Employee e = findOrThrow(id);
        validateUpdate(dto, e);
        DepartmentDTO refObj = null;
        Long _refId = resolveRefId(dto, false);
        if (_refId != null) {
            refObj = fetchRef(_refId);
            e.setDepartmentId(refObj.getDepartmentId());
        }
        if (dto.getFullName() != null) e.setFullName(clean(dto.getFullName()));
        if (dto.getEmail() != null) e.setEmail(clean(dto.getEmail()));
        if (dto.getPosition() != null) e.setPosition(normalizeEnum(dto.getPosition(), POSITION_VALUES));
        if (dto.getStatus() != null) e.setStatus(normalizeStatus(dto.getStatus()));
        if (dto.getStartDate() != null) e.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) e.setEndDate(dto.getEndDate());
        Employee saved = repo.save(e);
        return toDTO(saved, refObj == null ? fetchRef(saved.getDepartmentId()) : refObj);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDTO get(Long id) {
        Employee e = findOrThrow(id);
        return toDTO(e, fetchRef(e.getDepartmentId()));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Employee e = findOrThrow(id);
        e.setStatus(STATUS_INACTIVE);
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<EmployeeDTO> list(Integer page, Integer size, String name, String status) {
        int p = page == null ? 0 : page;
        int sz = size == null ? 10 : size;
        validatePagination(p, sz);
        String filterName = (name == null || name.isBlank()) ? null : name.trim().toLowerCase(Locale.ROOT);
        String filterStatus = (status == null || status.isBlank()) ? null : normalizeStatus(status);
        Specification<Employee> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (filterName != null) ps.add(cb.like(cb.lower(root.get("fullName")), "%" + filterName + "%"));
            if (filterStatus != null) ps.add(cb.equal(root.get("status"), filterStatus));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<EmployeeDTO> result = repo.findAll(spec, PageRequest.of(p, sz, Sort.by("employeeId").ascending()))
                .map(e -> toDTO(e, fetchRef(e.getDepartmentId())));
        return new PageDTO<>(result);
    }

    private void validateCreate(EmployeeDTO dto) {
        if (dto == null) throw validationFailed();
        validateRequiredString(dto.getFullName(), 100);
        validateRequiredString(dto.getEmail(), 100);
        validateEmail(dto.getEmail());
        validateRequiredString(dto.getPosition(), 30);
        if (dto.getPosition() != null) normalizeEnum(dto.getPosition(), POSITION_VALUES);
        validateStatusIfPresent(dto.getStatus());
        if (dto.getStartDate() == null) throw validationFailed();
        validateGe(dto.getEndDate(), dto.getStartDate());
        resolveRefId(dto, true);
    }

    private void validateUpdate(EmployeeDTO dto, Employee current) {
        if (dto == null) throw validationFailed();
        if (dto.getFullName() != null) {
            validateRequiredString(dto.getFullName(), 100);
        }
        if (dto.getEmail() != null) {
            validateRequiredString(dto.getEmail(), 100);
            validateEmail(dto.getEmail());
        }
        if (dto.getPosition() != null) {
            validateRequiredString(dto.getPosition(), 30);
            if (dto.getPosition() != null) normalizeEnum(dto.getPosition(), POSITION_VALUES);
        }
        validateStatusIfPresent(dto.getStatus());
        validateGe(dto.getEndDate() != null ? dto.getEndDate() : current.getEndDate(), dto.getStartDate() != null ? dto.getStartDate() : current.getStartDate());
    }

    private Employee findOrThrow(Long id) {
        if (id == null || id <= 0) throw notFound();
        return repo.findById(id).orElseThrow(this::notFound);
    }

    private Long resolveRefId(EmployeeDTO dto, boolean required) {
        Long id = null;
        if (dto.getDepartment() != null) {
            id = dto.getDepartment().getDepartmentId();
            // A present but incomplete nested object is invalid, not an omitted
            // field in a partial PUT.
            if (id == null) throw validationFailed();
        }
        if (id == null) {
            if (required) throw validationFailed();
            return null;
        }
        if (id <= 0) throw validationFailed();
        return id;
    }

    private DepartmentDTO fetchRef(Long id) {
        try {
            ApiResponseDTO<Object> r = refClient.getDepartment(id);
            if (r == null || r.getStatus() != ResponseStatuses.SUCCESS || r.getData() == null) throw refNotFound();
            return mapper.convertValue(r.getData(), DepartmentDTO.class);
        } catch (FeignException.BadRequest | FeignException.NotFound ex) {
            throw refNotFound();
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            throw new BusinessException(ResponseStatuses.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Reference service error");
        }
    }

    private BusinessException refNotFound() {
        return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "DepartmentId is not found");
    }

    private EmployeeDTO toDTO(Employee e, DepartmentDTO refObj) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(e.getEmployeeId());
        dto.setFullName(e.getFullName());
        dto.setEmail(e.getEmail());
        dto.setPosition(e.getPosition());
        dto.setStatus(e.getStatus());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setDepartment(refObj);
        return dto;
    }

    private void validateRequiredString(String v, int max) {
        if (v == null || v.trim().isEmpty() || (max > 0 && v.trim().length() > max)) throw validationFailed();
    }

    private void validateEmail(String v) {
        if (v != null && !EMAIL_PATTERN.matcher(v.trim()).matches()) throw validationFailed();
    }

    private String normalizeStatus(String x) {
        String n = clean(x).toUpperCase(Locale.ROOT);
        if (!STATUS_VALUES.contains(n)) throw validationFailed();
        return n;
    }

    private void validateStatusIfPresent(String x) {
        if (x != null) normalizeStatus(x);
    }

    private String normalizeEnum(String v, Set<String> allowed) {
        String c = clean(v);
        for (String a : allowed) if (a.equalsIgnoreCase(c)) return a;
        throw validationFailed();
    }

    private void validateGe(java.util.Date later, java.util.Date earlier) {
        if (later == null || earlier == null) return;
        if (toLocal(later).isBefore(toLocal(earlier))) throw validationFailed();
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

    private BusinessException validationFailed() {
        return new BusinessException(ResponseStatuses.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Data validation failed");
    }

    private BusinessException notFound() {
        return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "Employee is not found");
    }
}
