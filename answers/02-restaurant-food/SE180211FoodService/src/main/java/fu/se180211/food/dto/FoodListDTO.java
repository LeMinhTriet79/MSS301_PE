package fu.se180211.food.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class FoodListDTO {
    private int pageSize;
    private int pageNo;
    private int totalPages;
    private boolean first;
    private boolean last;
    private List<FoodResponseDTO> foods;

    public FoodListDTO() {
    }

    public FoodListDTO(Page<FoodResponseDTO> p) {
        pageSize = p.getSize();
        pageNo = p.getNumber();
        totalPages = p.getTotalPages();
        first = p.isFirst();
        last = p.isLast();
        foods = p.getContent();
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int v) {
        pageSize = v;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int v) {
        pageNo = v;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int v) {
        totalPages = v;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean v) {
        first = v;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean v) {
        last = v;
    }

    public List<FoodResponseDTO> getFoods() {
        return foods;
    }

    public void setFoods(List<FoodResponseDTO> v) {
        foods = v;
    }
}
