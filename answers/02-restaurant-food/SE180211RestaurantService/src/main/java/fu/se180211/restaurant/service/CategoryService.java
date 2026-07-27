package fu.se180211.restaurant.service;

import fu.se180211.restaurant.dto.CategoryDTO;
import fu.se180211.restaurant.dto.PageDTO;

public interface CategoryService {
    PageDTO<CategoryDTO> list(Integer page, Integer size);
}
