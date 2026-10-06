package com.bluecrystal.common.domain;

import java.util.List;

/**
 * 分页结果。
 *
 * @param total 总条数
 * @param pageNo 当前页码
 * @param pageSize 每页条数
 * @param list 当前页数据
 */
public record PageDTO<T>(long total, int pageNo, int pageSize, List<T> list) {

    public static <T> PageDTO<T> of(long total, int pageNo, int pageSize, List<T> list) {
        return new PageDTO<>(total, pageNo, pageSize, list == null ? List.of() : list);
    }
}
