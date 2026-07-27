package fu.se180211.restaurant.service.impl;

import fu.se180211.restaurant.common.BusinessException;
import fu.se180211.restaurant.common.ResponseStatuses;
import fu.se180211.restaurant.dto.CategoryDTO;
import fu.se180211.restaurant.dto.PageDTO;
import fu.se180211.restaurant.entity.Category;
import fu.se180211.restaurant.repository.CategoryRepository;
import fu.se180211.restaurant.service.CategoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repo;

    public CategoryServiceImpl(CategoryRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<CategoryDTO> list(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int sz = size == null ? 10 : size;
        if (p < 0 || sz < 1 || sz > 100)
            throw new BusinessException(ResponseStatuses.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Data validation failed");
        return new PageDTO<>(repo.findAll(PageRequest.of(p, sz, Sort.by("categoryId"))).map(this::toDTO));
    }

    private CategoryDTO toDTO(Category e) {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(e.getCategoryId());
        dto.setName(e.getName());
        return dto;
    }
}
