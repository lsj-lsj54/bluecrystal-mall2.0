package com.bluecrystal.item.domain.query;

import com.bluecrystal.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 商品分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ItemPageQuery extends PageQuery {

    /** 商品名称，模糊匹配。 */
    private String name;

    private String category;

    private String brand;
}
