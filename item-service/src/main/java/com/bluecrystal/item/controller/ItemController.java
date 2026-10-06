package com.bluecrystal.item.controller;

import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.common.domain.PageDTO;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.item.domain.dto.ItemStockDeductDTO;
import com.bluecrystal.item.domain.query.ItemPageQuery;
import com.bluecrystal.item.domain.vo.ItemVO;
import com.bluecrystal.item.service.IItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商品接口")
@Validated
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

  private final IItemService itemService;

  @Operation(summary = "查询商品详情")
  @GetMapping("/{id}")
  public R<ItemVO> queryById(@PathVariable("id") Long id) {
    return R.ok(toVO(itemService.queryById(id)));
  }

  @Operation(summary = "批量查询商品（供购物车/订单补全信息）")
  @GetMapping
  public R<List<ItemVO>> queryByIds(@RequestParam("ids") List<Long> ids) {
    return R.ok(itemService.queryByIds(ids).stream().map(this::toVO).toList());
  }

  @Operation(summary = "商品分页查询")
  @GetMapping("/page")
  public R<PageDTO<ItemVO>> page(ItemPageQuery query) {
    return R.ok(itemService.pageQuery(query));
  }

  @Operation(summary = "扣减库存（由 trade-service 在全局事务中调用）")
  @PostMapping("/stock/deduct")
  public R<Void> deductStock(@Valid @RequestBody List<ItemStockDeductDTO> details) {
    itemService.deductStock(details);
    return R.ok();
  }

  /** DTO → 前端 VO（字段对齐，直接透传）。 */
  private ItemVO toVO(ItemDTO dto) {
    return new ItemVO(
        dto.id(),
        dto.name(),
        dto.price(),
        dto.image(),
        dto.spec(),
        dto.stock(),
        dto.sold());
  }
}
