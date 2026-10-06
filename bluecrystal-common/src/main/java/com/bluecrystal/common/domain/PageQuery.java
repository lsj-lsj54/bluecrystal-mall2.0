package com.bluecrystal.common.domain;

import lombok.Data;

/**
 * 分页查询入参基类，业务查询对象可继承它，例如 ItemPageQuery extends PageQuery。
 */
@Data
public class PageQuery {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /** 页码，从 1 开始。 */
    private Integer pageNo = 1;

    /** 每页条数，最大 100。 */
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    public int getPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /** MyBatis-Plus 分页插件的偏移量。 */
    public long offset() {
        return (long) (getPageNo() - 1) * getPageSize();
    }
}
