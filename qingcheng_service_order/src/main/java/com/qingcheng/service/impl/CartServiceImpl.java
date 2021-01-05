package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ClassName:CartServiceImpl
 * Package:com.qingcheng.service.impl
 * Description:
 *
 * @Date:2020/5/31 11:31
 * @Author:jiaqi@163.com
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Reference
    private SkuService skuService;
    @Reference
    private CategoryService categoryService;
    @Autowired
    private PreferentialService preferentialService;

    @Override
    //    从redis获取某用户购物车列表
    public List<Map<String, Object>> findCartList(String username) {
        List<Map<String, Object>> cartList = (List<Map<String, Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        if (cartList == null) {
            cartList = new ArrayList<>(); //如果购物车为null，实例化一下
        }
        return cartList;
    }

//  向购物车添加商品（用户名，商品id，数量）
//    如果购物车中存在该商品，则累加，如果不存在，添加订单项
    @Override
    public void addItem(String username, String skuId, Integer num) {
        List<Map<String, Object>> cartList = findCartList(username); //通过用户名获取购物车列表
        boolean flag = false; //购物车中是否存在该商品
        for (Map<String, Object> map : cartList) {
            OrderItem orderItem = (OrderItem) map.get("item");
            if (orderItem.getSkuId().equals(skuId)) { //将购物车中的skuId逐个和当前要添加的skuId对比

                if (orderItem.getNum() <= 0) { //防止下面公式出现意外
                    cartList.remove(map);
                    break;
                }
                int weight = orderItem.getWeight() / orderItem.getNum(); //每件商品重量

                orderItem.setNum(orderItem.getNum() + num); //数量变更
                orderItem.setMoney(orderItem.getPrice() * orderItem.getNum()); //金额变更
                orderItem.setWeight(weight * orderItem.getNum()); //重量变更
                if (orderItem.getNum() <= 0) {
                    cartList.remove(map);
                }
                flag = true;
                break;
            }
        }
        if (flag = false) { //购物车中没有，那就添加
            Sku sku = skuService.findById(skuId);
            if (sku == null) {
                throw new RuntimeException("商品不存在");
            }
            if (!"1".equals(sku.getStatus())) {
                throw new RuntimeException("商品状态不合法");
            }
            if (num <= 0) {
                throw new RuntimeException("商品数量不合法");
            }

            OrderItem orderItem = new OrderItem();

            orderItem.setSkuId(sku.getId());
            orderItem.setName(sku.getName());
            orderItem.setNum(num);
            orderItem.setImage(sku.getImage());
            orderItem.setPrice(sku.getPrice());
            orderItem.setMoney(sku.getPrice()*num);
            orderItem.setSpuId(sku.getSpuId());
            if (sku.getWeight() == null) {
                sku.setWeight(0);
            }
            orderItem.setWeight(sku.getWeight()*num);

//         商品分类
            orderItem.setCategoryId3(sku.getCategoryId()); //3级分类id放入orderItem
            Category category3 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(sku.getCategoryId()); //3级分类
            if (category3 == null) {
                category3 = categoryService.findById(sku.getCategoryId()); //数据库查询3级分类
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(sku.getCategoryId(),category3); //3级分类对象放入redis
            }
            orderItem.setCategoryId2(category3.getParentId()); //2级分类id放入orderItem

            Category category2 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(category3.getParentId()); //2级分类
            if (category2 == null) {
                category2 = categoryService.findById(category3.getParentId());
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(category3.getParentId(),category2);
            }
            orderItem.setCategoryId1(category2.getParentId()); //1级分类id放入orderItem

            Map map = new HashMap();
            map.put("item", orderItem);
            map.put("cheched", false); //默认不选中
        }

        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
    }

    @Override
    //保存勾选状态
    public boolean updateChecked(String username, String skuId, boolean checked) {
        List<Map<String, Object>> cartList = findCartList(username);
        boolean isOk = false; //是否执行成功
        for (Map<String, Object> map : cartList) {
            OrderItem orderItem = (OrderItem) map.get("item");
            if (orderItem.getSkuId().equals(skuId)) {
                map.put("checked", checked);
                isOk = true;
                break;
            }
        }
        if (isOk) { //执行成功，更新到缓存
            redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
        }
        return true;
    }

    @Override
    //删除勾选商品。用未勾选的来覆盖当前购物车
    public void deleteCheckedCart(String username) {
        List<Map<String, Object>> cartList = findCartList(username).stream().filter(cart -> (boolean) cart.get("checked") == false).collect(Collectors.toList());
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
    }

    @Override
    //计算购物车的优惠金额
    public int preferential(String username) {

        //获取选中的订单项。List<Map> -> List<OrderItem>
        List<OrderItem> orderItemList = findCartList(username).stream().filter(cart -> (boolean) cart.get("checked") == true)
                .map(cart -> (OrderItem)cart.get("item")).collect(Collectors.toList());

         /*按分类Id聚合统计每个分类的消费金额
            key          value
         categoryId3    消费金额
           1            222
           2            333
        ......  */
        Map<Integer, IntSummaryStatistics> cartMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OrderItem::getCategoryId3, Collectors.summarizingInt(OrderItem::getMoney)));

        //循环结果，统计每个分类的优惠金额，并累加
        int allPreMoney = 0;    //总优惠金额
        for (Integer categoryId : cartMap.keySet()) {
            int money = (int) cartMap.get(categoryId).getSum(); //每个分类的消费金额
            int preMoney = preferentialService.findPreMoneyByCategoryId(categoryId, money); //每个分类的优惠金额
            System.out.println("分类：" + categoryId + "消费金额：" + money + "优惠金额：" + preMoney);
            allPreMoney += preMoney;
        }

        return allPreMoney;
    }

    @Override
    //在购物车页面点击“结算”进入结算页面后，获取最新的购物车列表
    public List<Map<String, Object>> findNewOrderItemList(String username) {
        List<Map<String, Object>> cartList = findCartList(username);
        for (Map<String, Object> cart : cartList) {
            OrderItem orderItem = (OrderItem) cart.get("item");
            Sku sku = skuService.findById(orderItem.getSkuId());
            orderItem.setPrice(sku.getPrice()); //更新单价
            orderItem.setMoney(sku.getPrice() * orderItem.getNum()); //更新该订单项总金额
        }
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username, cartList);
        return cartList;
    }
}
