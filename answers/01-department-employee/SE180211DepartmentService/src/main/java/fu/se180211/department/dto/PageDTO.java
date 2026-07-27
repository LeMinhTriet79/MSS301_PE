package fu.se180211.department.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageDTO<T> {
    private int size;
    private int page;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;
    private List<T> content;

    public PageDTO() {
    }

    public PageDTO(Page<T> p) {
        this.size = p.getSize();
        this.page = p.getNumber();
        this.totalPages = p.getTotalPages();
        this.totalElements = p.getTotalElements();
        this.first = p.isFirst();
        this.last = p.isLast();
        this.content = p.getContent();
    }

    public int getSize() {
        return size;
    }

    public void setSize(int v) {
        this.size = v;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int v) {
        this.page = v;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int v) {
        this.totalPages = v;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long v) {
        this.totalElements = v;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean v) {
        this.first = v;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean v) {
        this.last = v;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> v) {
        this.content = v;
    }
}
