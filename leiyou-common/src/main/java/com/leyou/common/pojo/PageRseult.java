package com.leyou.common.pojo;

import java.util.List;

public class PageRseult<T> {

    private Long total;
    private Integer totalPage;
    private List<T> items;

    public PageRseult(){}

    public PageRseult(
            Long total,List<T>items
    ){
        this.total = total;
        this.items = items;
    }

    public PageRseult(
            Long total,List<T>items,Integer totalPage
    ){
        this.total = total;
        this.items = items;
        this.totalPage = totalPage;
    }
    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
