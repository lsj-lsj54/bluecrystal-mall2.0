package com.bluecrystal.common.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bluecrystal.common.domain.PageDTO;

import java.util.List;
import java.util.function.Function;

/**
 * MyBatis-Plus 分页结果到 {@link PageDTO} 的转换工具。
 *
 * <p>放在 mybatis 包内是为了让 {@link PageDTO} 保持对 MyBatis-Plus 零依赖，网关也能安全使用。
 */
public final class PageConverter {

    private PageConverter() {
    }

    public static <E, T> PageDTO<T> toPageDTO(IPage<E> page, Function<E, T> mapper) {
        List<T> records = page.getRecords().stream().map(mapper).toList();
        return PageDTO.of(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), records);
    }
}
