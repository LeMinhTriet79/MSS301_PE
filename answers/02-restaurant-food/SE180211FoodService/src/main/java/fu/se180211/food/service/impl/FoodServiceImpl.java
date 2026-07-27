package fu.se180211.food.service.impl;

import feign.FeignException;
import fu.se180211.food.common.BusinessException;
import fu.se180211.food.common.ResponseStatuses;
import fu.se180211.food.config.RestaurantClient;
import fu.se180211.food.dto.ApiResponseDTO;
import fu.se180211.food.dto.FoodDTO;
import fu.se180211.food.dto.FoodListDTO;
import fu.se180211.food.dto.FoodResponseDTO;
import fu.se180211.food.dto.RestaurantDTO;
import fu.se180211.food.entity.Food;
import fu.se180211.food.repository.FoodRepository;
import fu.se180211.food.service.FoodService;
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
public class FoodServiceImpl implements FoodService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> STATUS_VALUES = Set.of("ACTIVE", "INACTIVE");
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final FoodRepository repo;
    private final RestaurantClient refClient;

    public FoodServiceImpl(FoodRepository repo, RestaurantClient refClient) {
        this.repo = repo;
        this.refClient = refClient;
    }

    @Override
    @Transactional
    public FoodDTO create(FoodDTO dto) {
        validateCreate(dto);
        Long _refId = resolveRefId(dto, true);
        RestaurantDTO refObj = fetchRef(_refId);
        Food e = new Food();
        e.setName(clean(dto.getName()));
        e.setPrice(dto.getPrice());
        e.setIngredients(clean(dto.getIngredients()));
        e.setStatus(normalizeStatus(dto.getStatus()));
        e.setRestaurantId(refObj.getRestaurantId());
        return toDTO(repo.save(e));
    }

    @Override
    @Transactional
    public FoodDTO update(Long id, FoodDTO dto) {
        Food e = findOrThrow(id);
        validateUpdate(dto);
        RestaurantDTO refObj = null;
        Long _refId = resolveRefId(dto, false);
        if (_refId != null) {
            refObj = fetchRef(_refId);
            e.setRestaurantId(refObj.getRestaurantId());
        }
        if (dto.getName() != null) e.setName(clean(dto.getName()));
        if (dto.getPrice() != null) e.setPrice(dto.getPrice());
        if (dto.getIngredients() != null) e.setIngredients(clean(dto.getIngredients()));
        if (dto.getStatus() != null) e.setStatus(normalizeStatus(dto.getStatus()));
        return toDTO(repo.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public FoodResponseDTO get(Long id) {
        Food e = findOrThrow(id);
        return toResponseDTO(e, fetchRef(e.getRestaurantId()));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Food e = findOrThrow(id);
        e.setStatus(STATUS_INACTIVE);
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodListDTO list(Integer page, Integer size, String name, String ingredients) {
        int p = page == null ? 0 : page;
        int sz = size == null ? 10 : size;
        validatePagination(p, sz);
        String filterName = (name == null || name.isBlank()) ? null : name.trim().toLowerCase(Locale.ROOT);
        String filterIngredients = (ingredients == null || ingredients.isBlank()) ? null : ingredients.trim().toLowerCase(Locale.ROOT);
        Specification<Food> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (filterName != null) ps.add(cb.like(cb.lower(root.get("name")), "%" + filterName + "%"));
            if (filterIngredients != null)
                ps.add(cb.like(cb.lower(root.get("ingredients")), "%" + filterIngredients + "%"));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<FoodResponseDTO> result = repo.findAll(spec, PageRequest.of(p, sz, Sort.by("foodId").ascending()))
                .map(e -> toResponseDTO(e, fetchRef(e.getRestaurantId())));
        return new FoodListDTO(result);
    }

    private void validateCreate(FoodDTO dto) {
        if (dto == null) throw validationFailed();
        validateRequiredString(dto.getName(), 100);
        if (dto.getPrice() == null) throw validationFailed();
        validateRequiredString(dto.getIngredients(), 500);
        if (dto.getStatus() == null) throw validationFailed();
        validateStatusIfPresent(dto.getStatus());
        resolveRefId(dto, true);
    }

    private void validateUpdate(FoodDTO dto) {
        if (dto == null) throw validationFailed();
        if (dto.getName() != null) {
            validateRequiredString(dto.getName(), 100);
        }
        if (dto.getIngredients() != null) {
            validateRequiredString(dto.getIngredients(), 500);
        }
        validateStatusIfPresent(dto.getStatus());
    }

    private Food findOrThrow(Long id) {
        if (id == null || id <= 0) throw notFound();
        return repo.findById(id).orElseThrow(this::notFound);
    }

    private Long resolveRefId(FoodDTO dto, boolean required) {
        Long id = dto.getRestaurantId();
        if (id == null) {
            if (required) throw validationFailed();
            return null;
        }
        if (id <= 0) throw validationFailed();
        return id;
    }

    private RestaurantDTO fetchRef(Long id) {
        try {
            ApiResponseDTO<RestaurantDTO> r = refClient.getRestaurant(id);
            if (r == null || r.getStatus() != ResponseStatuses.SUCCESS || r.getData() == null) throw refNotFound();
            return r.getData();
        } catch (FeignException.BadRequest | FeignException.NotFound ex) {
            throw refNotFound();
        } catch (BusinessException ex) {
            throw ex;
        } catch (FeignException ex) {
            throw new BusinessException(ResponseStatuses.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private BusinessException refNotFound() {
        return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "Restaurant ID is not found");
    }

    private FoodDTO toDTO(Food e) {
        FoodDTO dto = new FoodDTO();
        dto.setFoodId(e.getFoodId());
        dto.setName(e.getName());
        dto.setPrice(e.getPrice());
        dto.setIngredients(e.getIngredients());
        dto.setStatus(e.getStatus());
        dto.setRestaurantId(e.getRestaurantId());
        return dto;
    }

    private FoodResponseDTO toResponseDTO(Food e, RestaurantDTO refObj) {
        FoodResponseDTO dto = new FoodResponseDTO();
        dto.setFoodId(e.getFoodId());
        dto.setName(e.getName());
        dto.setPrice(e.getPrice());
        dto.setIngredients(e.getIngredients());
        dto.setRestaurant(refObj);
        return dto;
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

    private BusinessException notFound() {
        return new BusinessException(ResponseStatuses.NOT_FOUND, HttpStatus.BAD_REQUEST, "Food is not found");
    }
}
