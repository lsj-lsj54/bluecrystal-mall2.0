package com.bluecrystal.item.controller;

import com.bluecrystal.common.domain.PageDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.item.domain.query.ItemPageQuery;
import com.bluecrystal.item.domain.vo.ItemVO;
import com.bluecrystal.item.service.IItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索接口，网关白名单中的 {@code /search/**} 会路由到这里。
 *
 * <p>骨架先用数据库 like 查询实现，接入 Elasticsearch 时替换 service 实现即可。
 */
@Tag(name = "商品搜索")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final IItemService itemService;

    @Operation(summary = "关键字搜索商品")
    @GetMapping
    public R<PageDTO<ItemVO>> search(ItemPageQuery query) {
        return R.ok(itemService.pageQuery(query));
    }
}
