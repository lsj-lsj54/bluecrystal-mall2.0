package com.bluecrystal.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecrystal.api.client.ItemClient;
import com.bluecrystal.api.dto.ItemDTO;
import com.bluecrystal.cart.domain.dto.CartFormDTO;
import com.bluecrystal.cart.domain.po.Cart;
import com.bluecrystal.cart.domain.vo.CartVO;
import com.bluecrystal.cart.mapper.CartMapper;
import com.bluecrystal.cart.service.ICartService;
import com.bluecrystal.common.domain.R;
import com.bluecrystal.common.exception.BadRequestException;
import com.bluecrystal.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车业务实现。
 *
 * <p>设计要点：
 * <ul>
 *   <li>所有读写都以 {@link UserContext} 中的当前用户为维度，避免越权操作他人购物车；</li>
 *   <li>购物车只保存商品快照，名称/价格/规格/图片缺失时批量调用 item-service 补全，
 *       商品服务不可用时降级为“保留快照原值”，保证列表接口始终可用；</li>
 *   <li>加购不做商品存在性校验，价格与库存以下单时的实时校验为准。</li>
 * </ul>
 *
 * <p>TODO 后续接入 Redis：购物车读多写少，可改成 Hash 结构缓存 + 异步落库，
 * 并把商品快照缓存到 Redis 以减少对 item-service 的批量查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements ICartService {

    private final CartMapper cartMapper;

    private final ItemClient itemClient;

    @Override
    public List<CartVO> listOfCurrentUser() {
        Long userId = UserContext.requireUser();
        // 按 id 倒序返回，最近加入的商品排在前面
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getId));
        if (CollectionUtils.isEmpty(carts)) {
            return List.of();
        }
        Map<Long, ItemDTO> itemMap = queryItemMap(carts);
        return carts.stream()
                .map(cart -> toVO(cart, itemMap.get(cart.getItemId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdate(CartFormDTO form) {
        Integer num = form.num();
        if (num == null || num <= 0) {
            throw new BadRequestException("购买数量必须为正数");
        }
        Long userId = UserContext.requireUser();
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getItemId, form.itemId()));
        if (cart == null) {
            Cart entity = new Cart();
            entity.setUserId(userId);
            entity.setItemId(form.itemId());
            entity.setNum(num);
            // 商品名称/价格等快照留空，查询时按需向 item-service 补全，避免加购链路强依赖商品服务
            cartMapper.insert(entity);
            log.info("新增购物车条目：userId={}, itemId={}, num={}", userId, form.itemId(), num);
            return;
        }
        // TODO 高并发场景可改为 num = num + ? 的原子更新，接入 Redis 后由缓存承担累加
        cart.setNum(cart.getNum() + num);
        cartMapper.updateById(cart);
        log.info("累加购物车数量：userId={}, itemId={}, totalNum={}", userId, form.itemId(), cart.getNum());
    }

    @Override
    public void removeByItemIds(List<Long> itemIds) {
        if (CollectionUtils.isEmpty(itemIds)) {
            return;
        }
        Long userId = UserContext.requireUser();
        int rows = cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getItemId, itemIds));
        log.info("移除购物车商品：userId={}, itemIds={}, rows={}", userId, itemIds, rows);
    }

    /**
     * 批量查询缺失的商品信息。
     *
     * <p>只挑出确实缺字段的商品，且对 itemId 去重；远程调用失败时返回空 Map，
     * 由调用方保留购物车中的快照值。
     */
    private Map<Long, ItemDTO> queryItemMap(List<Cart> carts) {
        List<Long> itemIds = carts.stream()
                .filter(this::needFillItemInfo)
                .map(Cart::getItemId)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        try {
            R<List<ItemDTO>> result = itemClient.queryItemsByIds(itemIds);
            if (result == null || !result.success() || CollectionUtils.isEmpty(result.data())) {
                log.warn("补全购物车商品信息失败，保留快照原值：itemIds={}", itemIds);
                return Map.of();
            }
            return result.data().stream()
                    .filter(item -> item != null && item.id() != null)
                    .collect(Collectors.toMap(ItemDTO::id, Function.identity(), (first, second) -> first));
        } catch (Exception e) {
            // 调用商品服务异常时降级，不影响购物车列表返回
            log.warn("调用商品服务异常，保留快照原值：itemIds={}", itemIds, e);
            return Map.of();
        }
    }

    /** 商品名称/规格/图片任一为空，或价格为空，都需要补全。 */
    private boolean needFillItemInfo(Cart cart) {
        return !StringUtils.hasText(cart.getName())
                || !StringUtils.hasText(cart.getSpec())
                || !StringUtils.hasText(cart.getImage())
                || cart.getPrice() == null;
    }

    /** 转换视图对象：已有快照值优先，仅用商品服务的数据补齐缺失字段。 */
    private CartVO toVO(Cart cart, ItemDTO item) {
        String name = cart.getName();
        String spec = cart.getSpec();
        String image = cart.getImage();
        Integer price = cart.getPrice();
        if (item != null) {
            name = StringUtils.hasText(name) ? name : item.name();
            spec = StringUtils.hasText(spec) ? spec : item.spec();
            image = StringUtils.hasText(image) ? image : item.image();
            price = price != null ? price : item.price();
        }
        return new CartVO(cart.getId(), cart.getItemId(), cart.getNum(), name, spec, price, image);
    }
}
