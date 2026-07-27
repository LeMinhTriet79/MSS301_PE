package fu.se180211.restaurant.service.impl;

import fu.se180211.restaurant.common.BusinessException;
import fu.se180211.restaurant.common.ResponseStatuses;
import fu.se180211.restaurant.dto.PageDTO;
import fu.se180211.restaurant.dto.RestaurantDTO;
import fu.se180211.restaurant.entity.Restaurant;
import fu.se180211.restaurant.repository.CategoryRepository;
import fu.se180211.restaurant.repository.RestaurantRepository;
import fu.se180211.restaurant.service.RestaurantService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> STATUS_VALUES = Set.of("ACTIVE", "INACTIVE");
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final RestaurantRepository repo;
    private final CategoryRepository lookupRepo;

    public RestaurantServiceImpl(RestaurantRepository repo, CategoryRepository lookupRepo) {
        this.repo = repo;
        this.lookupRepo = lookupRepo;
    }

    @Override
    @Transactional
    public RestaurantDTO create(RestaurantDTO dto) {
        validateCreate(dto);
        validateLookup(dto.getCategoryId());
        String _u = clean(dto.getName());
        if (repo.existsByNameIgnoreCase(_u)) throw duplicate();
        Restaurant e = new Restaurant();
        e.setName(_u);
        e.setOwner(clean(dto.getOwner()));
        e.setAddress(clean(dto.getAddress()));
        e.setOpenDate(dto.getOpenDate());
        e.setPriceFrom(dto.getPriceFrom());
        e.setPriceTo(dto.getPriceTo());
        e.setPhone(clean(dto.getPhone()));
        e.setCategoryId(dto.getCategoryId());
        e.setStatus(normalizeStatus(dto.getStatus()));
        return toDTO(repo.save(e));
    }

    @Override
    @Transactional
    public RestaurantDTO update(Long id, RestaurantDTO dto) {
        Restaurant e = findOrThrow(id);
        validateUpdate(dto);
        if (dto.getCategoryId() != null) validateLookup(dto.getCategoryId());
        if (dto.getName() != null) {
            String candidate = clean(dto.getName());
            if (repo.existsByNameIgnoreCaseAndRestaurantIdNot(candidate, id)) throw duplicate();
        }
        if (dto.getName() != null) e.setName(clean(dto.getName()));
        if (dto.getOwner() != null) e.setOwner(clean(dto.getOwner()));
        if (dto.getAddress() != null) e.setAddress(clean(dto.getAddress()));
        if (dto.getOpenDate() != null) e.setOpenDate(dto.getOpenDate());
        if (dto.getPriceFrom() != null) e.setPriceFrom(dto.getPriceFrom());
        if (dto.getPriceTo() != null) e.setPriceTo(dto.getPriceTo());
        if (dto.getPhone() != null) e.setPhone(clean(dto.getPhone()));
        if (dto.getStatus() != null) e.setStatus(normalizeStatus(dto.getStatus()));
        if (dto.getCategoryId() != null) e.setCategoryId(dto.getCategoryId());
        return toDTO(repo.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDTO get(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Restaurant e = findOrThrow(id);
        e.setStatus(STATUS_INACTIVE);
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<RestaurantDTO> list(Integer page, Integer size, String name, String ownerName) {
        int p = page == null ? 0 : page;
        int sz = size == null ? 10 : size;
        validatePagination(p, sz);
        String filterName = (name == null || name.isBlank()) ? null : name.trim().toLowerCase(Locale.ROOT);
        String filterOwnerName = (ownerName == null || ownerName.isBlank()) ? null : ownerName.trim().toLowerCase(Locale.ROOT);
        Specification<Restaurant> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (filterName != null) ps.add(cb.like(cb.lower(root.get("name")), "%" + filterName + "%"));
            if (filterOwnerName != null) ps.add(cb.like(cb.lower(root.get("owner")), "%" + filterOwnerName + "%"));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<RestaurantDTO> result = repo.findAll(spec, PageRequest.of(p, sz, Sort.by("restaurantId").ascending()))
                .map(this::toDTO);
        return new PageDTO<>(result);
    }

    private void validateCreate(RestaurantDTO dto) {
        if (dto == null) throw validationFailed();
        validateRequiredString(dto.getName(), 100);
        validateRequiredString(dto.getOwner(), 100);
        validateRequiredString(dto.getAddress(), 100);
        if (dto.getOpenDate() == null) throw validationFailed();
        validateRequiredString(dto.getPhone(), 11);
        if (dto.getStatus() == null) throw validationFailed();
        validateStatusIfPresent(dto.getStatus());
        if (dto.getCategoryId() == null) throw validationFailed();
    }

    private void validateUpdate(RestaurantDTO dto) {
        if (dto == null) throw validationFailed();
        if (dto.getName() != null) {
            validateRequiredString(dto.getName(), 100);
        }
        if (dto.getOwner() != null) {
            validateRequiredString(dto.getOwner(), 100);
        }
        if (dto.getAddress() != null) {
            validateRequiredString(dto.getAddress(), 100);
        }
        if (dto.getPhone() != null) {
            validateRequiredString(dto.getPhone(), 11);
        }
        validateStatusIfPresent(dto.getStatus());
    }

    private Restaurant findOrThrow(Long id) {
        if (id == null || id <= 0) throw notFound();
        return repo.findById(id).orElseThrow(this::notFound);
    }

    private RestaurantDTO toDTO(Restaurant e) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setRestaurantId(e.getRestaurantId());
        dto.setName(e.getName());
        dto.setOwner(e.getOwner());
        dto.setAddress(e.getAddress());
        dto.setOpenDate(e.getOpenDate());
        dto.setPriceFrom(e.getPriceFrom());
        dto.setPriceTo(e.getPriceTo());
        dto.setPhone(e.getPhone());
        dto.setStatus(e.getStatus());
        dto.setCategoryId(e.getCategoryId());
        return dto;
    }

    private void validateLookup(Long id) {
        if (id == null || id <= 0 || !lookupRepo.existsById(id))
            throw new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "Category ID is not found");
    }

    private void validateRequiredString(String v, int max) {
        if (v == null || v.trim().isEmpty() || (max > 0 && v.trim().length() > max)) throw validationFailed();
    }

    private String normalizeStatus(String x) {
        String n = clean(x).toUpperCase(Locale.ROOT);
        if (!STATUS_VALUES.contains(n)) throw validationFailed();
        return n;
    }

    private void validateStatusIfPresent(String x) {
        if (x != null) normalizeStatus(x);
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

    private BusinessException duplicate() {
        return new BusinessException(ResponseStatuses.DUPLICATE_CODE, HttpStatus.BAD_REQUEST, "Name is duplicated");
    }

    private BusinessException notFound() {
        return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "Restaurant is not found");
    }
}
